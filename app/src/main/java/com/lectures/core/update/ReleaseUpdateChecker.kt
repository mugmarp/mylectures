package com.lectures.core.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by foreground and periodic checks; only successful responses are cached. */
internal class ReleaseUpdateChecker(
    private val fetchJson: suspend (String) -> String,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Cached(val fetchedAt: Long, val info: AppUpdateInfo)
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, Cached>()

    suspend fun check(
        localName: String,
        localCode: Int,
        metadataUrl: String = InAppUpdateManager.DEFAULT_VERSION_METADATA_URL,
        forceRefresh: Boolean = false
    ): Result<AppUpdateInfo?> = mutex.withLock {
        try {
            require(ReleaseMetadata.isSecureHttpsUrl(metadataUrl)) { "Metadata URL must use HTTPS." }
            val timestamp = now()
            val existing = cache[metadataUrl]
            val info = if (!forceRefresh && existing != null && timestamp - existing.fetchedAt in 0 until CACHE_TTL_MS) {
                existing.info
            } else {
                val candidate = try {
                    ReleaseMetadata.parseVersionJson(fetchJson(metadataUrl))
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    try {
                        ReleaseMetadata.parseGitHubRelease(fetchJson(InAppUpdateManager.GITHUB_RELEASES_API_URL))
                    } catch (fallback: Exception) {
                        if (fallback is CancellationException) throw fallback
                        fallback.addSuppressed(e)
                        throw IllegalStateException("Could not check for updates. Check your connection and try again.", fallback)
                    }
                }
                currentCoroutineContext().ensureActive()
                // Bound cache size when a configured metadata endpoint changes repeatedly.
                if (cache.size >= 8) cache.clear()
                cache[metadataUrl] = Cached(now(), candidate)
                candidate
            }
            currentCoroutineContext().ensureActive()
            Result.success(info.takeIf { ReleaseMetadata.isNewer(it, localName, localCode) })
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    companion object { private const val CACHE_TTL_MS = 15 * 60 * 1000L }
}
