package com.mustime

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mustime.core.storage.AttachmentStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [AttachmentStore], which persists note attachments into private storage.
 *
 * The FileProvider test is the important one: [AttachmentStore.contentUriFor]
 * swallows exceptions and returns null, so an authority that does not match the
 * manifest would surface to the user as "Attachment file is missing" rather than
 * as a crash. That makes the mismatch easy to ship, so it is asserted directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttachmentStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun cleanAttachments() {
        File(context.filesDir, "attachments").deleteRecursively()
    }

    @Test
    fun testCopyIntoPrivateStorageWritesFileAndReturnsRelativePath() {
        cleanAttachments()

        // A file:// source stands in for a picker URI: ContentResolver.openInputStream
        // handles both, so the copy path is exercised the same way.
        val source = File(context.cacheDir, "source_whiteboard.jpg").apply {
            writeBytes(ByteArray(2048) { it.toByte() })
        }

        val stored = AttachmentStore.copyIntoPrivateStorage(
            context = context,
            source = Uri.fromFile(source),
            fallbackName = "fallback.jpg"
        )

        assertNotNull("copy returned null for a readable source", stored)
        assertTrue("stored path must be relative", !stored!!.startsWith("/"))
        assertTrue("stored path must live under attachments/", stored.startsWith("attachments/"))

        val target = File(context.filesDir, stored)
        assertTrue("copied file does not exist", target.exists())
        assertEquals("copied file is not byte-identical", 2048L, target.length())

        // The source must remain untouched — the copy is what the note references.
        assertTrue("source file was consumed", source.exists())

        cleanAttachments()
    }

    @Test
    fun testCopyIntoPrivateStorageReturnsNullForUnreadableSource() {
        cleanAttachments()

        val missing = File(context.cacheDir, "does_not_exist_${System.nanoTime()}.pdf")
        val stored = AttachmentStore.copyIntoPrivateStorage(
            context = context,
            source = Uri.fromFile(missing),
            fallbackName = "fallback.pdf"
        )

        assertNull("unreadable source must not produce a stored path", stored)
    }

    @Test
    fun testCopyStripsPathSeparatorsFromHostileDisplayName() {
        cleanAttachments()

        val source = File(context.cacheDir, "hostile.bin").apply { writeBytes(ByteArray(16)) }

        // A display name cannot be injected here via file://, so the guard is
        // exercised through the fallback path instead: the resulting filename must
        // never contain a separator that could escape files/attachments/.
        val stored = AttachmentStore.copyIntoPrivateStorage(
            context = context,
            source = Uri.fromFile(source),
            fallbackName = "../../escaped.pdf"
        )

        assertNotNull(stored)
        val fileName = stored!!.substringAfterLast('/')
        assertFalse("filename must not contain a forward slash", fileName.contains('/'))
        assertFalse("filename must not contain a backslash", fileName.contains('\\'))
        assertFalse("filename must not traverse upward", fileName.contains(".."))

        // And the file really is inside the attachments directory.
        val attachmentsDir = File(context.filesDir, "attachments").canonicalPath
        assertTrue(
            "copied file escaped the attachments directory",
            File(context.filesDir, stored).canonicalPath.startsWith(attachmentsDir)
        )

        cleanAttachments()
    }

    @Test
    fun testResolveFileReturnsNullForMissingAndLegacyRows() {
        cleanAttachments()

        assertNull("null path must resolve to null", AttachmentStore.resolveFile(context, null))
        assertNull("blank path must resolve to null", AttachmentStore.resolveFile(context, ""))
        assertNull(
            "missing file must resolve to null",
            AttachmentStore.resolveFile(context, "attachments/nope.pdf")
        )
    }

    @Test
    fun testDeleteRemovesBackingFileAndToleratesMissing() {
        cleanAttachments()

        val source = File(context.cacheDir, "to_delete.pdf").apply { writeBytes(ByteArray(32)) }
        val stored = AttachmentStore.copyIntoPrivateStorage(
            context, Uri.fromFile(source), "to_delete.pdf"
        )
        assertNotNull(stored)
        val target = File(context.filesDir, stored!!)
        assertTrue(target.exists())

        AttachmentStore.delete(context, stored)
        assertFalse("file still present after delete", target.exists())

        // Deleting again (or deleting a legacy null path) must not throw.
        AttachmentStore.delete(context, stored)
        AttachmentStore.delete(context, null)
        AttachmentStore.delete(context, "")
    }

    /**
     * The authority computed in code must match the FileProvider declared in the
     * manifest, otherwise opening an attachment fails silently.
     */
    @Test
    fun testFileProviderAuthorityMatchesManifestDeclaration() {
        cleanAttachments()

        val source = File(context.cacheDir, "open_me.pdf").apply { writeBytes(ByteArray(64)) }
        val stored = AttachmentStore.copyIntoPrivateStorage(
            context, Uri.fromFile(source), "open_me.pdf"
        )
        assertNotNull(stored)

        val uri = AttachmentStore.contentUriFor(context, stored!!)
        assertNotNull(
            "contentUriFor returned null — FileProvider authority likely does not " +
                "match the manifest declaration (expected " +
                "\"${context.packageName}.fileprovider\")",
            uri
        )

        assertEquals(
            "${context.packageName}.fileprovider",
            uri!!.authority
        )

        // The authority must be resolvable, i.e. actually declared in the manifest.
        val provider = context.packageManager.resolveContentProvider(uri.authority!!, 0)
        assertNotNull(
            "no provider declared for authority ${uri.authority}",
            provider
        )

        cleanAttachments()
    }
}
