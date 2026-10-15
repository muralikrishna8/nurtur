package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.FeedLog
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class FeedCsvExportRequest(
    val feeds: List<FeedLog>,
    val zoneId: ZoneId,
    val today: LocalDate
)

data class FeedCsvDocument(
    val fileName: String,
    val contents: String
)

sealed class FeedCsvParseResult {
    data class Ready(val feeds: List<FeedLog>) : FeedCsvParseResult()
    data object Invalid : FeedCsvParseResult()
}

object FeedCsvCodec {
    const val MAX_CSV_CHARS = 2_000_000
    const val MAX_FEED_ROWS = 10_000

    private const val BREASTMILK = "Breastmilk"
    private const val FORMULA = "Formula"
    private const val MIN_OFFERED_ML = 1
    private const val MAX_OFFERED_ML = 1000
    private const val MAX_NOTES_LENGTH = 280
    private const val HEADER_ROW_COUNT = 1
    private val headerColumns = listOf(
        "start_time",
        "end_time",
        "amount_offered_ml",
        "amount_consumed_ml",
        "milk_type",
        "notes"
    )
    private val csvMimeTypes = setOf(
        "text/csv",
        "text/comma-separated-values",
        "application/csv"
    )
    private val formulaTriggers = setOf('=', '+', '-', '@', '\t', '\r')
    private val exportFileNamePattern =
        Regex("""nurtur_\d{2}-\d{2}-\d{4}_\d{2}-\d{2}-\d{4}-data\.csv""")
    private val fileDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US)

    fun export(request: FeedCsvExportRequest): FeedCsvDocument {
        val orderedFeeds = request.feeds.sortedWith(
            compareBy({ it.startTime }, { it.endTime }, { it.id })
        )
        return FeedCsvDocument(
            fileName = fileName(orderedFeeds, request.zoneId, request.today),
            contents = encode(orderedFeeds, request.zoneId)
        )
    }

    fun parse(csvText: String): FeedCsvParseResult {
        val rows = readRows(csvText) ?: return FeedCsvParseResult.Invalid
        if (rows.isEmpty() || rows.first().map { it.trim() } != headerColumns) {
            return FeedCsvParseResult.Invalid
        }
        val dataRows = rows.drop(HEADER_ROW_COUNT)
        if (dataRows.isEmpty() || dataRows.size > MAX_FEED_ROWS) {
            return FeedCsvParseResult.Invalid
        }
        val feeds = dataRows.map { row -> parseFeed(row) ?: return FeedCsvParseResult.Invalid }
        return FeedCsvParseResult.Ready(feeds)
    }

    fun feedsNotAlreadyStored(existing: List<FeedLog>, incoming: List<FeedLog>): List<FeedLog> {
        val remaining = existing.groupingBy { it.toIdentity() }.eachCount().toMutableMap()
        val novel = ArrayList<FeedLog>(incoming.size)
        for (feed in incoming) {
            val identity = feed.toIdentity()
            val available = remaining[identity] ?: 0
            if (available > 0) {
                remaining[identity] = available - 1
                continue
            }
            novel += feed.copy(id = 0L, remoteId = null)
        }
        return novel
    }

    fun isSafeExportFileName(fileName: String): Boolean {
        return exportFileNamePattern.matches(fileName)
    }

    fun isImportableCsv(displayName: String?, mimeType: String?): Boolean {
        val name = displayName
            ?.trim()
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.trim()
        if (!name.isNullOrEmpty()) {
            return name.lowercase(Locale.US).endsWith(".csv") && !name.contains('\u0000')
        }
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
        return mime in csvMimeTypes
    }

    private fun fileName(feeds: List<FeedLog>, zoneId: ZoneId, today: LocalDate): String {
        val instants = feeds.flatMap { listOf(it.startTime, it.endTime) }
        val startDate = instants.minOrNull()?.toLocalDate(zoneId) ?: today
        val endDate = instants.maxOrNull()?.toLocalDate(zoneId) ?: today
        return "nurtur_${fileDateFormatter.format(startDate)}_${fileDateFormatter.format(endDate)}-data.csv"
    }

    private fun encode(feeds: List<FeedLog>, zoneId: ZoneId): String {
        val builder = StringBuilder()
        builder.append(headerColumns.joinToString(",")).append('\n')
        feeds.forEach { feed ->
            builder.append(encodeFeed(feed, zoneId)).append('\n')
        }
        return builder.toString()
    }

    private fun encodeFeed(feed: FeedLog, zoneId: ZoneId): String {
        val cells = listOf(
            formatTimestamp(feed.startTime, zoneId),
            formatTimestamp(feed.endTime, zoneId),
            feed.amountOffered.toString(),
            feed.amountConsumed.toString(),
            feed.milkType,
            feed.notes.orEmpty()
        )
        return cells.joinToString(",") { escapeCell(it) }
    }

    private fun formatTimestamp(epochMillis: Long, zoneId: ZoneId): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        )
    }

    private fun escapeCell(value: String): String {
        val neutralized = if (value.isNotEmpty() && value[0] in formulaTriggers) {
            "'$value"
        } else {
            value
        }
        val mustQuote = neutralized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!mustQuote) {
            return neutralized
        }
        return "\"${neutralized.replace("\"", "\"\"")}\""
    }

    private fun parseFeed(row: List<String>): FeedLog? {
        if (row.size != headerColumns.size) {
            return null
        }
        val startTime = parseTimestamp(row[0]) ?: return null
        val endTime = parseTimestamp(row[1]) ?: return null
        val offered = row[2].trim().toIntOrNull() ?: return null
        val consumed = row[3].trim().toIntOrNull() ?: return null
        val milkType = canonicalMilkType(row[4]) ?: return null
        val notes = when (val parsedNotes = parseNotes(row[5])) {
            NotesParse.Invalid -> return null
            is NotesParse.Value -> parsedNotes.notes
        }
        if (startTime <= 0L || endTime <= 0L || endTime < startTime) {
            return null
        }
        if (offered !in MIN_OFFERED_ML..MAX_OFFERED_ML || consumed !in 0..offered) {
            return null
        }
        return FeedLog(
            id = 0L,
            remoteId = null,
            startTime = startTime,
            endTime = endTime,
            amountOffered = offered,
            amountConsumed = consumed,
            milkType = milkType,
            notes = notes
        )
    }

    private fun parseTimestamp(raw: String): Long? {
        return try {
            OffsetDateTime.parse(raw.trim()).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun canonicalMilkType(raw: String): String? {
        return when (raw.trim().lowercase(Locale.US)) {
            "breastmilk" -> BREASTMILK
            "formula" -> FORMULA
            else -> null
        }
    }

    private fun parseNotes(raw: String): NotesParse {
        val restored = restoreFormulaPrefix(raw.trim())
        if (restored.length > MAX_NOTES_LENGTH) {
            return NotesParse.Invalid
        }
        return NotesParse.Value(restored.ifBlank { null })
    }

    private fun restoreFormulaPrefix(value: String): String {
        if (value.length >= 2 && value[0] == '\'' && value[1] in formulaTriggers) {
            return value.substring(1)
        }
        return value
    }

    private fun readRows(csvText: String): List<List<String>>? {
        if (csvText.contains('\u0000') || csvText.length > MAX_CSV_CHARS) {
            return null
        }
        val text = csvText.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val current = text[index]
            if (inQuotes) {
                if (current == '"') {
                    if (text.getOrNull(index + 1) == '"') {
                        cell.append('"')
                        index += 2
                        continue
                    }
                    inQuotes = false
                    index += 1
                    continue
                }
                cell.append(current)
                index += 1
                continue
            }
            when (current) {
                '"' -> {
                    if (cell.isNotEmpty()) {
                        return null
                    }
                    inQuotes = true
                    index += 1
                }
                ',' -> {
                    row += cell.toString()
                    cell.clear()
                    index += 1
                }
                '\n', '\r' -> {
                    if (current == '\r' && text.getOrNull(index + 1) == '\n') {
                        index += 1
                    }
                    row += cell.toString()
                    cell.clear()
                    appendRow(rows, row)
                    row.clear()
                    if (rows.size > MAX_FEED_ROWS + HEADER_ROW_COUNT) {
                        return null
                    }
                    index += 1
                }
                else -> {
                    cell.append(current)
                    index += 1
                }
            }
        }
        if (inQuotes) {
            return null
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            appendRow(rows, row)
        }
        if (rows.size > MAX_FEED_ROWS + HEADER_ROW_COUNT) {
            return null
        }
        return rows
    }

    private fun appendRow(rows: MutableList<List<String>>, row: List<String>) {
        if (row.all { it.isBlank() }) {
            return
        }
        rows += row.toList()
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }

    private fun FeedLog.toIdentity(): FeedIdentity {
        return FeedIdentity(
            startTime = startTime,
            endTime = endTime,
            amountOffered = amountOffered,
            amountConsumed = amountConsumed,
            milkType = milkType,
            notes = notes
        )
    }

    private data class FeedIdentity(
        val startTime: Long,
        val endTime: Long,
        val amountOffered: Int,
        val amountConsumed: Int,
        val milkType: String,
        val notes: String?
    )

    private sealed class NotesParse {
        data class Value(val notes: String?) : NotesParse()
        data object Invalid : NotesParse()
    }
}
