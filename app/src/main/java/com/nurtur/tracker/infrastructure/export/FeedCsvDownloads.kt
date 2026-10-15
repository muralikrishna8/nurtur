package com.nurtur.tracker.infrastructure.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import com.nurtur.tracker.domain.service.FeedCsvCodec
import com.nurtur.tracker.domain.service.FeedCsvDocument
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

sealed class FeedCsvReadResult {
    data class Text(val value: String) : FeedCsvReadResult()
    data object NotCsv : FeedCsvReadResult()
    data object Unreadable : FeedCsvReadResult()
}

object FeedCsvDownloads {
    private const val CSV_MIME_TYPE = "text/csv"
    private const val READ_CHUNK_BYTES = 8 * 1024

    fun save(context: Context, document: FeedCsvDocument): Boolean {
        if (!FeedCsvCodec.isSafeExportFileName(document.fileName)) {
            return false
        }
        val bytes = document.contents.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > FeedCsvCodec.MAX_CSV_CHARS) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, document.fileName, bytes)
            } else {
                saveLegacyDownloads(document.fileName, bytes)
            }
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun read(context: Context, uri: Uri): FeedCsvReadResult {
        return try {
            val displayName = queryDisplayName(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            if (!FeedCsvCodec.isImportableCsv(displayName, mimeType)) {
                return FeedCsvReadResult.NotCsv
            }
            val sizeBytes = querySizeBytes(context, uri)
            if (sizeBytes != null && sizeBytes > FeedCsvCodec.MAX_CSV_CHARS) {
                return FeedCsvReadResult.Unreadable
            }
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                readBounded(input)
            } ?: return FeedCsvReadResult.Unreadable
            FeedCsvReadResult.Text(text)
        } catch (_: IOException) {
            FeedCsvReadResult.Unreadable
        } catch (_: SecurityException) {
            FeedCsvReadResult.Unreadable
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(context: Context, fileName: String, bytes: ByteArray): Boolean {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        runCatching {
            resolver.delete(
                collection,
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(fileName)
            )
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, CSV_MIME_TYPE)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return false
        return try {
            val output = resolver.openOutputStream(uri)
            if (output == null) {
                resolver.delete(uri, null, null)
                return false
            }
            output.use { stream -> stream.write(bytes) }
            val published = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, published, null, null)
            true
        } catch (_: IOException) {
            resolver.delete(uri, null, null)
            false
        }
    }

    private fun saveLegacyDownloads(fileName: String, bytes: ByteArray): Boolean {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!directory.exists() && !directory.mkdirs()) {
            return false
        }
        val target = File(directory, fileName)
        val canonicalDirectory = directory.canonicalFile
        val canonicalTarget = target.canonicalFile
        val directoryPrefix = canonicalDirectory.path + File.separator
        if (!canonicalTarget.path.startsWith(directoryPrefix)) {
            return false
        }
        canonicalTarget.writeBytes(bytes)
        return true
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return queryColumn(context, uri, OpenableColumns.DISPLAY_NAME)
    }

    private fun querySizeBytes(context: Context, uri: Uri): Long? {
        val raw = queryColumn(context, uri, OpenableColumns.SIZE) ?: return null
        val size = raw.toLongOrNull() ?: return null
        if (size < 0L) {
            return null
        }
        return size
    }

    private fun queryColumn(context: Context, uri: Uri, column: String): String? {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return null
            }
            val index = cursor.getColumnIndex(column)
            if (index < 0 || cursor.isNull(index)) {
                return null
            }
            return cursor.getString(index)
        }
        return null
    }

    private fun readBounded(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        var total = 0
        while (true) {
            val read = input.read(chunk)
            if (read < 0) {
                break
            }
            total += read
            if (total > FeedCsvCodec.MAX_CSV_CHARS) {
                return null
            }
            buffer.write(chunk, 0, read)
        }
        return String(buffer.toByteArray(), StandardCharsets.UTF_8)
    }
}
