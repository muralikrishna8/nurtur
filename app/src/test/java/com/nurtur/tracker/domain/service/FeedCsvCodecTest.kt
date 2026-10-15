package com.nurtur.tracker.domain.service

import com.nurtur.tracker.domain.model.FeedLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FeedCsvCodecTest {
    private val utc = ZoneId.of("UTC")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun test_export_fileNameUsesEarliestAndLatestDates() {
        // Arrange
        val earlier = feed(
            startTime = utcMillis("2026-01-15T00:00:00Z"),
            endTime = utcMillis("2026-01-15T00:30:00Z")
        )
        val later = feed(
            startTime = utcMillis("2026-10-14T23:00:00Z"),
            endTime = utcMillis("2026-10-14T23:30:00Z")
        )

        // Act
        val utcDocument = FeedCsvCodec.export(request(listOf(later, earlier), utc))
        val kolkataDocument = FeedCsvCodec.export(request(listOf(later, earlier), kolkata))

        // Assert
        assertEquals("nurtur_15-01-2026_14-10-2026-data.csv", utcDocument.fileName)
        assertEquals("nurtur_15-01-2026_15-10-2026-data.csv", kolkataDocument.fileName)
    }

    @Test
    fun test_export_emptyHistoryUsesTodayForBothDates() {
        // Arrange
        val request = request(emptyList(), utc)

        // Act
        val document = FeedCsvCodec.export(request)

        // Assert
        assertEquals("nurtur_14-10-2026_14-10-2026-data.csv", document.fileName)
        assertEquals(
            "start_time,end_time,amount_offered_ml,amount_consumed_ml,milk_type,notes\n",
            document.contents
        )
    }

    @Test
    fun test_parse_roundTripsFeedFieldsAndDropsIdentity() {
        // Arrange
        val original = feed(
            notes = "sleepy, \"very\" fine\nnext line"
        )
        val csv = FeedCsvCodec.export(request(listOf(original), kolkata)).contents

        // Act
        val parsed = FeedCsvCodec.parse(csv)

        // Assert
        assertTrue(parsed is FeedCsvParseResult.Ready)
        val restored = (parsed as FeedCsvParseResult.Ready).feeds.single()
        assertEquals(0L, restored.id)
        assertEquals(null, restored.remoteId)
        assertEquals(original.startTime, restored.startTime)
        assertEquals(original.endTime, restored.endTime)
        assertEquals(original.amountOffered, restored.amountOffered)
        assertEquals(original.amountConsumed, restored.amountConsumed)
        assertEquals(original.milkType, restored.milkType)
        assertEquals(original.notes, restored.notes)
    }

    @Test
    fun test_parse_restoresFormulaPrefixedNotes() {
        // Arrange
        val original = feed(notes = "=1+1")
        val csv = FeedCsvCodec.export(request(listOf(original), utc)).contents

        // Act
        val parsed = FeedCsvCodec.parse(csv)

        // Assert
        assertTrue(csv.contains("'=1+1"))
        val restored = (parsed as FeedCsvParseResult.Ready).feeds.single()
        assertEquals("=1+1", restored.notes)
    }

    @Test
    fun test_parse_acceptsCrlfAndBlankNotes() {
        // Arrange
        val original = feed(notes = null)
        val csv = FeedCsvCodec.export(request(listOf(original), utc)).contents.replace("\n", "\r\n")

        // Act
        val parsed = FeedCsvCodec.parse("\uFEFF$csv")

        // Assert
        val restored = (parsed as FeedCsvParseResult.Ready).feeds.single()
        assertEquals(null, restored.notes)
        assertEquals("Formula", restored.milkType)
    }

    @Test
    fun test_parse_rejectsInvalidRowWithoutPartialSuccess() {
        // Arrange
        val valid = FeedCsvCodec.export(request(listOf(feed()), utc)).contents.trimEnd()
        val invalidRow = valid.lineSequence().last().replace("Formula", "Water")
        val csv = valid + "\n" + invalidRow + "\n"

        // Act
        val parsed = FeedCsvCodec.parse(csv)

        // Assert
        assertTrue(parsed is FeedCsvParseResult.Invalid)
    }

    @Test
    fun test_parse_rejectsConsumedAboveOffered() {
        // Arrange
        val csv = header() + "2026-10-14T10:00:00Z,2026-10-14T10:20:00Z,100,120,Formula,\n"

        // Act
        val parsed = FeedCsvCodec.parse(csv)

        // Assert
        assertTrue(parsed is FeedCsvParseResult.Invalid)
    }

    @Test
    fun test_parse_rejectsHeaderOnlyFile() {
        // Act
        val parsed = FeedCsvCodec.parse(header())

        // Assert
        assertTrue(parsed is FeedCsvParseResult.Invalid)
    }

    @Test
    fun test_feedsNotAlreadyStored_keepsOnlyNewCopies() {
        // Arrange
        val existing = feed(id = 9L)
        val duplicate = existing.copy(id = 0L, remoteId = null)
        val novel = feed(
            id = 3L,
            startTime = existing.startTime + 3_600_000L,
            endTime = existing.endTime + 3_600_000L,
            notes = "later feed"
        )

        // Act
        val toInsert = FeedCsvCodec.feedsNotAlreadyStored(
            existing = listOf(existing),
            incoming = listOf(duplicate, novel)
        )

        // Assert
        assertEquals(1, toInsert.size)
        assertEquals(0L, toInsert.single().id)
        assertEquals(null, toInsert.single().remoteId)
        assertEquals(novel.startTime, toInsert.single().startTime)
    }

    @Test
    fun test_isImportableCsv_allowsCsvNamesAndKnownMimeTypesOnly() {
        // Assert
        assertTrue(FeedCsvCodec.isImportableCsv("history.CSV", "application/octet-stream"))
        assertTrue(FeedCsvCodec.isImportableCsv(null, "text/csv"))
        assertTrue(FeedCsvCodec.isImportableCsv("Downloads/nurtur.csv", null))
        assertFalse(FeedCsvCodec.isImportableCsv("history.txt", "text/csv"))
        assertFalse(FeedCsvCodec.isImportableCsv("history.csv.exe", "text/csv"))
        assertFalse(FeedCsvCodec.isImportableCsv(null, "application/octet-stream"))
    }

    @Test
    fun test_isSafeExportFileName_matchesDatePatternOnly() {
        // Assert
        assertTrue(FeedCsvCodec.isSafeExportFileName("nurtur_01-03-2026_14-10-2026-data.csv"))
        assertFalse(FeedCsvCodec.isSafeExportFileName("../nurtur_01-03-2026_14-10-2026-data.csv"))
        assertFalse(FeedCsvCodec.isSafeExportFileName("nurtur_1-03-2026_14-10-2026-data.csv"))
    }

    private fun request(feeds: List<FeedLog>, zoneId: ZoneId): FeedCsvExportRequest {
        return FeedCsvExportRequest(feeds = feeds, zoneId = zoneId, today = today)
    }

    private fun header(): String {
        return "start_time,end_time,amount_offered_ml,amount_consumed_ml,milk_type,notes\n"
    }

    private fun utcMillis(isoInstant: String): Long {
        return java.time.Instant.parse(isoInstant).toEpochMilli()
    }

    private fun feed(
        id: Long = 4L,
        startTime: Long = utcMillis("2026-03-01T08:15:00Z"),
        endTime: Long = utcMillis("2026-03-01T08:35:00Z"),
        notes: String? = "sleepy, but fine"
    ): FeedLog {
        return FeedLog(
            id = id,
            remoteId = "remote",
            startTime = startTime,
            endTime = endTime,
            amountOffered = 120,
            amountConsumed = 90,
            milkType = "Formula",
            notes = notes
        )
    }
}
