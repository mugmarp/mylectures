package com.lectures.core.update

/** A published release; a tag-only response has no Android build code. */
data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val versionCode: Int? = null,
    val isMandatory: Boolean = false
)
