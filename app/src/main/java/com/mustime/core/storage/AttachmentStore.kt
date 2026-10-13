package com.mustime.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/**
 * Persists note attachments into the app's private storage.
 *
 * A content URI returned by a document/photo picker only carries a transient
 * read grant. Copying the bytes into `files/attachments/` at selection time
 * gives the note a durable reference that still works after a reboot, after
 * the picker's grant expires, and after the source file is moved or deleted.
 *
 * The database stores the path relative to [Context.getFilesDir] (for example
 * `attachments/1731_slides.pdf`), so the value stays valid even though the
 * absolute data directory path is not stable across installs.
 */
object AttachmentStore {

    private const val DIR_NAME = "attachments"

    /** Directory holding copied attachments, created on demand. */
    private fun attachmentDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /** Display name reported by the picker, or null when unavailable. */
    fun queryDisplayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }

    /** Size reported by the picker in bytes, or null when unavailable. */
    fun querySize(context: Context, uri: Uri): Long? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (idx != -1 && cursor.moveToFirst()) cursor.getLong(idx) else null
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Copies [source] into private storage and returns the relative path to
     * store in the database, or null when the copy fails.
     *
     * The filename is prefixed with a timestamp so repeated attachments with
     * the same display name do not overwrite each other.
     */
    fun copyIntoPrivateStorage(
        context: Context,
        source: Uri,
        fallbackName: String
    ): String? {
        try {
            val displayName = queryDisplayName(context, source)
                ?.takeIf { it.isNotBlank() }
                ?: fallbackName

            // Strip path separators so a hostile display name cannot escape the dir.
            val safeName = displayName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .ifBlank { fallbackName }

            val target = File(attachmentDir(context), "${System.currentTimeMillis()}_$safeName")

            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            if (target.length() == 0L) {
                target.delete()
                return null
            }

            return "$DIR_NAME/${target.name}"
        } catch (_: Exception) {
            return null
        }
    }

    /** Absolute file for a stored relative path, or null when it is missing. */
    fun resolveFile(context: Context, relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    /**
     * Content URI suitable for handing to another app (viewer, share sheet).
     * Requires the FileProvider declared in AndroidManifest.xml.
     */
    fun contentUriFor(context: Context, relativePath: String): Uri? {
        val file = resolveFile(context, relativePath) ?: return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Deletes the backing file for [relativePath], ignoring a missing file. */
    fun delete(context: Context, relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        try {
            File(context.filesDir, relativePath).delete()
        } catch (_: Exception) {
            // Nothing actionable — the note row is removed regardless.
        }
    }
}
