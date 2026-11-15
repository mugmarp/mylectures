package com.lectures.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lectures.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Checks published releases and opens their installation instructions in a browser. */
object InAppUpdateManager {
    const val DEFAULT_VERSION_METADATA_URL = "https://raw.githubusercontent.com/mugmarp/mylectures/main/version.json"
    const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/mugmarp/mylectures/releases/latest"
    const val FALLBACK_RELEASES_WEB_URL = ReleaseMetadata.RELEASES_PAGE
    private const val MAX_METADATA_BYTES = 1_048_576

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .build()
    }
    private val checker by lazy { ReleaseUpdateChecker(::fetchJson) }

    suspend fun checkForUpdate(
        context: Context,
        customUrl: String? = null,
        forceRefresh: Boolean = false
    ): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        purgeStaleUpdateFiles(context)
        checker.check(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
            customUrl ?: DEFAULT_VERSION_METADATA_URL, forceRefresh)
    }

    fun openUpdatePage(context: Context, updateInfo: AppUpdateInfo): Result<Unit> = try {
        require(ReleaseMetadata.isReleasePage(updateInfo.releaseUrl)) { "Invalid Lectures release page." }
        require(ReleaseMetadata.isNewer(updateInfo, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) { "This release is not newer than the installed app." }
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.releaseUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    /** Old builds could leave an incomplete APK in this cache directory. */
    fun purgeStaleUpdateFiles(context: Context) {
        runCatching { File(context.cacheDir, "updates").takeIf { it.isDirectory }?.deleteRecursively() }
    }

    private suspend fun fetchJson(url: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Lectures-Updater/${BuildConfig.VERSION_NAME}")
            .build()
        val call = httpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = response.use {
                        check(it.isSuccessful) { "Update endpoint returned HTTP ${it.code}." }
                        check(it.request.url.isHttps) { "Update endpoint must use HTTPS." }
                        val body = checkNotNull(it.body) { "Empty update response." }
                        check(body.contentLength() <= MAX_METADATA_BYTES) { "Update metadata is too large." }
                        body.byteStream().use { input ->
                            val output = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = input.read(buffer)
                            while (count != -1) {
                                check(output.size() + count <= MAX_METADATA_BYTES) { "Update metadata is too large." }
                                output.write(buffer, 0, count)
                                count = input.read(buffer)
                            }
                            output.toString(Charsets.UTF_8.name())
                        }
                    }
                    if (continuation.isActive) continuation.resume(json)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        })
    }
}
