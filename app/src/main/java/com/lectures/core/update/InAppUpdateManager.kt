package com.lectures.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsState
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.regex.Pattern

/**
 * Manages in-app updates for sideloaded APK distribution.
 * 
 * Flow: Check version → Download APK to cache → Verify SHA-256 → 
 * Launch native install intent via FileProvider → System handles install.
 */
object InAppUpdateManager {

    /** Current update state observed by UI. */
    sealed interface UpdateState {
        data class Idle(val hasUpdate: Boolean = false, val versionInfo: VersionInfo? = null) : UpdateState
        data class Downloading(val progress: Float, val versionInfo: VersionInfo) : UpdateState
        data class Downloaded(val apkFile: File, val versionInfo: VersionInfo) : UpdateState
        data class Installing(val versionInfo: VersionInfo) : UpdateState
        data class Error(val message: String) : UpdateState
    }

    /** Version metadata from remote source. */
    data class VersionInfo(
        val versionCode: Int,
        val versionName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileSizeBytes: Long,
        val sha256: String,
        val isMandatory: Boolean = false
    )

    private const val UPDATE_DIR_NAME = "updates"
    private const val APK_FILENAME = "update.apk"
    private const val SHA256_ALGORITHM = "SHA-256"
    private const val BUFFER_SIZE = 8192

    /** Checks if app has permission to install packages. */
    fun canInstallApks(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /** Opens system settings for "Install unknown apps" permission. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * Fetches version metadata from remote JSON endpoint.
     * Returns VersionInfo if newer version available, null if up to date or error.
     */
    suspend fun fetchVersionInfo(versionJsonUrl: String, currentVersionCode: Int): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(versionJsonUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
            val versionInfo = parseVersionJson(jsonString)
            
            if (versionInfo.versionCode > currentVersionCode) {
                versionInfo
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVersionJson(json: String): VersionInfo {
        val versionCode = extractJsonInt(json, "versionCode") ?: 0
        val versionName = extractJsonString(json, "versionName") ?: ""
        val releaseNotes = extractJsonString(json, "releaseNotes") ?: ""
        val downloadUrl = extractJsonString(json, "downloadUrl") ?: ""
        val fileSizeBytes = extractJsonLong(json, "fileSizeBytes") ?: 0
        val sha256 = extractJsonString(json, "sha256") ?: ""
        val isMandatory = extractJsonBoolean(json, "isMandatory") ?: false

        return VersionInfo(
            versionCode = versionCode,
            versionName = versionName,
            releaseNotes = releaseNotes,
            downloadUrl = downloadUrl,
            fileSizeBytes = fileSizeBytes,
            sha256 = sha256,
            isMandatory = isMandatory
        )
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*(\\d+)")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1).toIntOrNull() else null
    }

    private fun extractJsonLong(json: String, key: String): Long? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*(\\d+)")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1).toLongOrNull() else null
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val pattern = Pattern.compile("\"$key\"\\s*:\\s*(true|false)")
        val matcher = pattern.matcher(json)
        return if (matcher.find()) matcher.group(1).toBoolean() else null
    }

    /**
     * Downloads APK to cache directory with progress callback.
     * Returns File if successful, null if failed.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        expectedSha256: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, UPDATE_DIR_NAME).apply { mkdirs() }
            val apkFile = File(updateDir, APK_FILENAME)
            if (apkFile.exists()) apkFile.delete()

            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true

            val contentLength = connection.contentLengthLong
            if (contentLength <= 0) return@withContext null

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    var count = input.read(buffer)
                    while (count != -1) {
                        output.write(buffer, 0, count)
                        total += count.toLong()
                        onProgress(total.toFloat() / contentLength.toFloat())
                        count = input.read(buffer)
                    }
                }
            }

            // Verify SHA-256
            if (!verifySha256(apkFile, expectedSha256)) {
                apkFile.delete()
                return@withContext null
            }

            apkFile
        } catch (e: Exception) {
            null
        }
    }

    private fun verifySha256(file: File, expectedSha256: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
            file.inputStream().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = input.read()
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read()
                }
            }
            val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            actualSha256.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /** Triggers system package installer for the downloaded APK. */
    fun triggerInstall(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Cleans up any existing downloaded APK. */
    fun cleanup(context: Context) {
        val updateDir = File(context.cacheDir, UPDATE_DIR_NAME)
        if (updateDir.exists()) {
            updateDir.deleteRecursively()
        }
    }
}

/**
 * UI helper for observing update state in Compose.
 */
class UpdateViewModel(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<InAppUpdateManager.UpdateState>(InAppUpdateManager.UpdateState.Idle())
    val state = _state.asStateFlow()

    private var job: Job? = null

    fun checkForUpdate(
        versionJsonUrl: String,
        currentVersionCode: Int
    ) {
        job?.cancel()
        job = scope.launch {
            val versionInfo = InAppUpdateManager.fetchVersionInfo(versionJsonUrl, currentVersionCode)
            _state.update { if (versionInfo != null) {
                InAppUpdateManager.UpdateState.Idle(true, versionInfo)
            } else {
                InAppUpdateManager.UpdateState.Idle(false, null)
            } }
        }
    }

    fun startDownload(versionInfo: InAppUpdateManager.VersionInfo, context: Context) {
        _state.update { InAppUpdateManager.UpdateState.Downloading(0f, versionInfo) }
        job?.cancel()
        job = scope.launch {
            val apkFile = InAppUpdateManager.downloadApk(
                context = context,
                downloadUrl = versionInfo.downloadUrl,
                expectedSha256 = versionInfo.sha256,
                onProgress = { progress ->
                    _state.update { InAppUpdateManager.UpdateState.Downloading(progress, versionInfo) }
                }
            )
            _state.update { if (apkFile != null) {
                InAppUpdateManager.UpdateState.Downloaded(apkFile, versionInfo)
            } else {
                InAppUpdateManager.UpdateState.Error("Download failed or checksum mismatch")
            } }
        }
    }

    fun install(context: Context, apkFile: File, versionInfo: InAppUpdateManager.VersionInfo) {
        _state.update { InAppUpdateManager.UpdateState.Installing(versionInfo) }
        InAppUpdateManager.triggerInstall(context, apkFile)
    }

    fun clearError() {
        _state.update { InAppUpdateManager.UpdateState.Idle() }
    }
}
