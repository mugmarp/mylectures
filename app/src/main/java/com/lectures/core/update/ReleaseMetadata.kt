package com.lectures.core.update

import org.json.JSONObject
import java.net.URI

/** Parses release contracts without deriving Android build codes from display tags. */
internal object ReleaseMetadata {
    const val RELEASES_PAGE = "https://github.com/mugmarp/mylectures/releases/latest"

    fun isSecureHttpsUrl(value: String?): Boolean = try {
        val uri = URI(value?.trim().orEmpty())
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443)
    } catch (_: Exception) { false }

    fun isReleasePage(value: String): Boolean = try {
        val uri = URI(value)
        isSecureHttpsUrl(value) && uri.host.equals("github.com", ignoreCase = true) &&
            (uri.path == "/mugmarp/mylectures/releases/latest" ||
                (Regex("/mugmarp/mylectures/releases/tag/[^/]+").matches(uri.path) &&
                    parseVersion(uri.path.substringAfterLast('/')) != null)) &&
            uri.rawQuery == null && uri.rawFragment == null
    } catch (_: Exception) { false }

    fun parseVersionJson(raw: String): AppUpdateInfo {
        val root = JSONObject(raw)
        val obj = if (root.has("app_update")) root.getJSONObject("app_update") else root
        val name = text(obj, "versionName", "latest_version_name")
        val version = requireNotNull(parseVersion(name)) { "Version metadata has an invalid release version." }
        require(version.preRelease.isEmpty()) { "Version metadata does not identify a stable release." }
        val codeValue = when {
            obj.has("versionCode") -> obj.get("versionCode")
            obj.has("latest_version_code") -> obj.get("latest_version_code")
            else -> null
        }
        val code = codeValue?.let {
            require(it is Number && it.toDouble() == it.toLong().toDouble() && it.toLong() in 1..Int.MAX_VALUE.toLong()) {
                "Version metadata has an invalid Android build code."
            }
            it.toInt()
        }
        val download = text(obj, "downloadUrl", "download_url")
        require(download.isBlank() || isSecureHttpsUrl(download)) { "Update URLs must use HTTPS." }
        val explicitPage = text(obj, "releaseUrl", "release_url")
        val page = when {
            explicitPage.isNotBlank() -> explicitPage
            isReleasePage(download) -> download
            else -> RELEASES_PAGE
        }
        require(isReleasePage(page)) { "Update page must be an official Lectures release page." }
        require(page == RELEASES_PAGE || compareVersions(URI(page).path.substringAfterLast('/'), name) == 0) {
            "Update page does not match the advertised version."
        }
        return AppUpdateInfo(
            versionName = name.trim().removePrefix("v"),
            versionCode = code,
            releaseNotes = text(obj, "releaseNotes", "release_notes"),
            releaseUrl = page,
            isMandatory = obj.optBoolean("isMandatory", obj.optBoolean("force_update", false))
        )
    }

    fun parseGitHubRelease(raw: String): AppUpdateInfo {
        val obj = JSONObject(raw)
        require(!obj.optBoolean("draft") && !obj.optBoolean("prerelease")) { "The release is not a published stable release." }
        val tag = obj.getString("tag_name")
        val version = requireNotNull(parseVersion(tag)) { "The published release tag has an invalid version." }
        require(version.preRelease.isEmpty()) { "The release tag identifies a prerelease." }
        val page = obj.getString("html_url")
        require(isReleasePage(page) && URI(page).path.substringAfterLast('/') == tag.trim()) {
            "Published release URL does not match its tag."
        }
        return AppUpdateInfo(tag.trim().removePrefix("v"), obj.optString("body", ""), page)
    }

    fun isNewer(info: AppUpdateInfo, localName: String, localCode: Int): Boolean {
        val comparison = compareVersions(info.versionName, localName) ?: return false
        // Android rejects a build-code downgrade even when its marketing version is higher.
        return if (info.versionCode != null) info.versionCode > localCode && comparison >= 0
        else comparison > 0
    }

    fun compareVersions(first: String, second: String): Int? {
        val a = parseVersion(first) ?: return null
        val b = parseVersion(second) ?: return null
        for (i in 0..2) {
            val comparison = a.parts[i].compareTo(b.parts[i])
            if (comparison != 0) return comparison
        }
        if (a.preRelease.isEmpty() && b.preRelease.isNotEmpty()) return 1
        if (a.preRelease.isNotEmpty() && b.preRelease.isEmpty()) return -1
        for (i in 0 until maxOf(a.preRelease.size, b.preRelease.size)) {
            val left = a.preRelease.getOrNull(i) ?: return -1
            val right = b.preRelease.getOrNull(i) ?: return 1
            val leftNumber = left.toLongOrNull()
            val rightNumber = right.toLongOrNull()
            val comparison = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    private data class Version(val parts: List<Long>, val preRelease: List<String>)
    private fun parseVersion(raw: String): Version? {
        val match = Regex("^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$").matchEntire(raw.trim()) ?: return null
        val parts = listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3].ifBlank { "0" })
            .map { it.toLongOrNull() ?: return null }
        val preRelease = match.groupValues[4].takeIf { it.isNotBlank() }?.split('.') ?: emptyList()
        if (preRelease.any { it.all(Char::isDigit) && (it.length > 1 && it.startsWith('0') || it.toLongOrNull() == null) }) return null
        return Version(parts, preRelease)
    }

    private fun text(obj: JSONObject, first: String, second: String): String =
        if (obj.has(first) && !obj.isNull(first)) obj.getString(first)
        else if (obj.has(second) && !obj.isNull(second)) obj.getString(second) else ""
}
