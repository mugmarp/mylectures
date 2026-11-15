package com.lectures

import android.app.Application
import android.app.NotificationManager
import android.app.Notification
import com.lectures.core.notification.NotificationHelper
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.update.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReleaseUpdateTest {
    private val metadataUrl = InAppUpdateManager.DEFAULT_VERSION_METADATA_URL
    private val githubUrl = InAppUpdateManager.GITHUB_RELEASES_API_URL
    private val page = "https://github.com/mugmarp/mylectures/releases/tag/v2.0.0"
    private fun metadata(name: String = "2.0.0", code: Int = 4) = JSONObject()
        .put("versionName", name).put("versionCode", code).put("releaseNotes", "New features")
        .put("downloadUrl", "https://github.com/mugmarp/mylectures/releases/download/v2.0.0/app.apk").toString()
    private fun github(name: String = "v2.0.0", url: String = page) = JSONObject()
        .put("tag_name", name).put("html_url", url).put("body", "Release notes")
        .put("draft", false).put("prerelease", false).toString()
    private fun info(name: String, code: Int? = null) = AppUpdateInfo(name, "Notes", page, code)
    private fun assertRejected(block: () -> Unit) {
        try { block(); fail("Invalid release metadata must be rejected") } catch (_: IllegalArgumentException) { }
    }

    @Test fun nestedMetadataPreservesEscapedNotesAndActualBuildCode() {
        val notes = "Quoted \"highlights\"\n- Offline notes\n- Alarms"
        val nested = JSONObject(metadata()).put("releaseNotes", notes)
        val parsed = ReleaseMetadata.parseVersionJson(JSONObject().put("app_update", nested).toString())
        assertEquals(notes, parsed.releaseNotes)
        assertEquals(4, parsed.versionCode)
        assertEquals(ReleaseMetadata.RELEASES_PAGE, parsed.releaseUrl)
    }

    @Test fun legacyAliasesUseOfficialReleasePageWithoutInventingBuildCodes() {
        val parsed = ReleaseMetadata.parseVersionJson("""{"latest_version_name":"v2.0.0","release_notes":"Notes","force_update":true}""")
        assertNull(parsed.versionCode)
        assertTrue(parsed.isMandatory)
        assertEquals("2.0.0", parsed.versionName)
        assertEquals(ReleaseMetadata.RELEASES_PAGE, parsed.releaseUrl)
    }

    @Test fun explicitOfficialReleasePageIsRetained() {
        val raw = JSONObject(metadata()).put("releaseUrl", page).toString()
        assertEquals(page, ReleaseMetadata.parseVersionJson(raw).releaseUrl)
    }

    @Test fun invalidBuildCodesAndUnstableVersionsAreRejected() {
        for (value in listOf(0, -1, 1.5, "4", Int.MAX_VALUE.toLong() + 1L)) {
            assertRejected { ReleaseMetadata.parseVersionJson(JSONObject(metadata()).put("versionCode", value).toString()) }
        }
        for (name in listOf("latest", "2.x.0", "v2.0.0-rc.1", "999999999999999999999.0.0")) {
            assertRejected { ReleaseMetadata.parseVersionJson(JSONObject(metadata()).put("versionName", name).toString()) }
        }
    }

    @Test fun insecureDownloadsAndUntrustedReleasePagesAreRejected() {
        assertRejected { ReleaseMetadata.parseVersionJson(JSONObject(metadata()).put("downloadUrl", "http://example.com/app.apk").toString()) }
        assertRejected { ReleaseMetadata.parseVersionJson(JSONObject(metadata()).put("releaseUrl", "https://example.com/release").toString()) }
    }

    @Test fun gitHubTagsNeverBecomeFabricatedAndroidBuildCodes() {
        val parsed = ReleaseMetadata.parseGitHubRelease(github())
        assertEquals("2.0.0", parsed.versionName)
        assertNull(parsed.versionCode)
        assertEquals(page, parsed.releaseUrl)
    }

    @Test fun unpublishedAndPrereleaseGitHubResponsesAreRejected() {
        assertRejected { ReleaseMetadata.parseGitHubRelease(JSONObject(github()).put("draft", true).toString()) }
        assertRejected { ReleaseMetadata.parseGitHubRelease(JSONObject(github()).put("prerelease", true).toString()) }
        assertRejected { ReleaseMetadata.parseGitHubRelease(github("v2.0.0-beta.1")) }
        assertRejected { ReleaseMetadata.parseGitHubRelease(github(url = "https://github.com/other/app/releases/tag/v2.0.0")) }
    }

    @Test fun urlValidationRejectsCredentialsInvalidHostsAndNonHttpsSchemes() {
        for (url in listOf("http://github.com", "file:///tmp/app.apk", "javascript:alert(1)", "https:///missing", "https://user:password@github.com/release", "https://github.com:8080/release", "https://github.com\\@evil.example/release")) {
            assertFalse(url, ReleaseMetadata.isSecureHttpsUrl(url))
        }
        assertTrue(ReleaseMetadata.isSecureHttpsUrl(metadataUrl))
        assertTrue(ReleaseMetadata.isReleasePage(page))
        assertFalse(ReleaseMetadata.isReleasePage("https://github.com.evil.example/mugmarp/mylectures/releases/latest"))
        assertFalse(ReleaseMetadata.isReleasePage("https://github.com/mugmarp/mylectures/releases/download/v2.0.0/app.apk"))
        assertFalse(ReleaseMetadata.isReleasePage("$page?redirect=https://evil.example"))
    }

    @Test fun semanticComparisonHandlesNumericSegmentsPrereleasesAndBuildMetadata() {
        assertTrue(ReleaseMetadata.compareVersions("2.10.0", "2.9.9")!! > 0)
        assertEquals(0, ReleaseMetadata.compareVersions("v1.1", "1.1.0"))
        assertTrue(ReleaseMetadata.compareVersions("2.0.0", "2.0.0-rc.1")!! > 0)
        assertTrue(ReleaseMetadata.compareVersions("2.0.0-rc.10", "2.0.0-rc.2")!! > 0)
        assertTrue(ReleaseMetadata.compareVersions("2.0.0-alpha", "2.0.0-1")!! > 0)
        assertEquals(0, ReleaseMetadata.compareVersions("2.0.0+build.10", "2.0.0+build.20"))
        assertNull(ReleaseMetadata.compareVersions("2.bad.0", "2.0.0"))
        assertNull(ReleaseMetadata.compareVersions("2.0.0-01", "2.0.0"))
    }

    @Test fun olderPublishedReleaseDoesNotPromptCurrentDevelopmentBuild() {
        assertFalse(ReleaseMetadata.isNewer(info("1.1.0"), "1.1.1", 3))
        assertFalse(ReleaseMetadata.isNewer(info("1.1.1"), "1.1.1", 3))
        assertTrue(ReleaseMetadata.isNewer(info("1.2.0"), "1.1.1", 3))
    }

    @Test fun knownBuildCodesMustIncreaseWithoutDowngradingMarketingVersion() {
        assertFalse(ReleaseMetadata.isNewer(info("2.0.0", 2), "1.1.1", 3))
        assertFalse(ReleaseMetadata.isNewer(info("2.0.0", 3), "1.1.1", 3))
        assertFalse(ReleaseMetadata.isNewer(info("1.1.0", 4), "1.1.1", 3))
        assertTrue(ReleaseMetadata.isNewer(info("1.1.1", 4), "1.1.1", 3))
    }

    @Test fun missingVersionFileFallsBackToPublishedGitHubRelease() = runBlocking {
        val calls = mutableListOf<String>()
        val checker = ReleaseUpdateChecker({ url -> calls.add(url); if (url == metadataUrl) throw IOException("HTTP 404") else github() })
        val result = checker.check("1.1.1", 3)
        assertTrue(result.isSuccess)
        assertEquals("2.0.0", result.getOrThrow()!!.versionName)
        assertEquals(listOf(metadataUrl, githubUrl), calls)
    }

    @Test fun validCurrentMetadataDoesNotNeedAFallbackRequest() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; metadata("1.1.1", 3) })
        assertNull(checker.check("1.1.1", 3).getOrThrow())
        assertEquals(1, calls)
    }

    @Test fun malformedPrimaryMetadataUsesValidatedFallback() = runBlocking {
        val checker = ReleaseUpdateChecker({ url -> if (url == metadataUrl) "not JSON" else github() })
        assertEquals("2.0.0", checker.check("1.1.1", 3).getOrThrow()!!.versionName)
    }

    @Test fun twoFailedEndpointsProduceAnErrorAndAreNotCached() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; throw IOException("Offline") })
        assertTrue(checker.check("1.1.1", 3).isFailure)
        assertTrue(checker.check("1.1.1", 3).isFailure)
        assertEquals(4, calls)
    }

    @Test fun invalidMetadataUrlNeverStartsANetworkRequest() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; metadata() })
        assertTrue(checker.check("1.1.1", 3, "http://example.com/version.json").isFailure)
        assertEquals(0, calls)
    }

    @Test fun successfulCacheIsRecomparedAgainstTheInstalledBuild() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; metadata() })
        assertNotNull(checker.check("1.1.1", 3).getOrThrow())
        assertNull(checker.check("2.0.0", 4).getOrThrow())
        assertEquals(1, calls)
    }

    @Test fun manualRefreshBypassesCacheAndTtlExpiresAtFifteenMinutes() = runBlocking {
        var calls = 0
        var time = 100L
        val checker = ReleaseUpdateChecker({ calls++; metadata() }, { time })
        checker.check("1.1.1", 3)
        checker.check("1.1.1", 3, forceRefresh = true)
        assertEquals(2, calls)
        time += 15 * 60 * 1000L
        checker.check("1.1.1", 3)
        assertEquals(3, calls)
    }

    @Test fun differentMetadataEndpointsDoNotShareACachedCandidate() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; metadata() })
        checker.check("1.1.1", 3, "https://example.com/first.json")
        checker.check("1.1.1", 3, "https://example.com/second.json")
        assertEquals(2, calls)
    }

    @Test fun cancelledChecksDoNotFallbackOrCacheASuccess() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; if (calls == 1) throw CancellationException("Stopped") else metadata() })
        try { checker.check("1.1.1", 3); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertNotNull(checker.check("1.1.1", 3).getOrThrow())
        assertEquals(2, calls)
    }

    @Test fun overlappingChecksShareOneSuccessfulFetch() = runBlocking {
        var calls = 0
        val checker = ReleaseUpdateChecker({ calls++; delay(10); metadata() })
        val first = async { checker.check("1.1.1", 3) }
        val second = async { checker.check("1.1.1", 3) }
        assertNotNull(first.await().getOrThrow())
        assertNotNull(second.await().getOrThrow())
        assertEquals(1, calls)
    }

    @Test fun releaseActionOpensOfficialPageAndRejectsAnOlderVersion() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(InAppUpdateManager.openUpdatePage(app, info("2.0.0", 99)).isSuccess)
        val intent = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(page, intent.data.toString())
        assertTrue(InAppUpdateManager.openUpdatePage(app, info("1.0.0")).isFailure)
        assertNull(shadowOf(app).nextStartedActivity)
        assertTrue(InAppUpdateManager.openUpdatePage(app, info("2.0.0").copy(releaseUrl = "https://example.com/release")).isFailure)
    }

    @Test fun missingBrowserIsReportedAsAFailedAction() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val noBrowser = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException("No browser") }
        }
        assertTrue(InAppUpdateManager.openUpdatePage(noBrowser, info("2.0.0")).isFailure)
    }

    @Test fun staleApkCleanupPreservesNoteAttachments() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val oldApk = File(context.cacheDir, "updates/partial.apk").apply { parentFile!!.mkdirs(); writeText("partial") }
        val attachment = File(context.filesDir, "attachments/lecture.pdf").apply { parentFile!!.mkdirs(); writeText("notes") }
        InAppUpdateManager.purgeStaleUpdateFiles(context)
        assertFalse(oldApk.exists())
        assertEquals("notes", attachment.readText())
    }

    @Test fun changelogExpansionRetainsMoreThanFourHighlightsAndSkipsFencedHashes() {
        val notes = "# Release\n" + (1..6).joinToString("\n") { "- **Improvement $it**" } + "\n```\nsecret hash\n```"
        val highlights = cleanReleaseHighlights(notes)
        assertEquals(6, highlights.size)
        assertEquals("Improvement 6", highlights.last())
        assertFalse(highlights.any { it.contains("secret hash") })
        assertEquals(1, cleanReleaseHighlights("").size)
    }
    @Test fun updateNotificationsUseReleaseWordingAndRespectDisabledNotifications() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        shadowOf(manager).setNotificationsEnabled(false)
        assertFalse(NotificationHelper.showAppUpdateNotification(context, "2.0.0"))
        assertTrue(manager.activeNotifications.isEmpty())
        shadowOf(manager).setNotificationsEnabled(true)
        assertTrue(NotificationHelper.showAppUpdateNotification(context, "2.0.0"))
        val notification = manager.activeNotifications.single().notification
        assertEquals("Version 2.0.0 is available", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("View Release", notification.actions.single().title.toString())
        assertEquals(4, shadowOf(notification.contentIntent).savedIntent.getIntExtra("EXTRA_NAV_TAB", -1))
    }

}
