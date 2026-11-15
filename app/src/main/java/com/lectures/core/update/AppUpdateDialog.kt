package com.lectures.core.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lectures.BuildConfig

@Composable
fun AppUpdateDialog(updateInfo: AppUpdateInfo, onDismiss: () -> Unit) {
    if (!ReleaseMetadata.isNewer(updateInfo, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) {
        LaunchedEffect(updateInfo) { onDismiss() }
        return
    }
    val context = LocalContext.current
    var error by rememberSaveable(updateInfo.releaseUrl) { mutableStateOf<String?>(null) }
    var expanded by rememberSaveable(updateInfo.releaseUrl) { mutableStateOf(false) }
    val highlights = remember(updateInfo.releaseNotes) { cleanReleaseHighlights(updateInfo.releaseNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
        title = { Text("App Update Available") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("v${updateInfo.versionName} · Current: v${BuildConfig.VERSION_NAME}")
                if (updateInfo.isMandatory) {
                    Text("This update is recommended. You can continue using your offline timetable.",
                        color = MaterialTheme.colorScheme.primary)
                }
                Text("What's new", fontWeight = FontWeight.Bold)
                highlights.take(if (expanded) highlights.size else 4).forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = MaterialTheme.colorScheme.primary)
                        Text(item, modifier = Modifier.weight(1f))
                    }
                }
                if (highlights.size > 4) {
                    Text(
                        if (expanded) "Show less" else "Show all highlights",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 8.dp)
                    )
                }
                Text("Open the release page for the download and installation instructions.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = null
                InAppUpdateManager.openUpdatePage(context, updateInfo).onFailure {
                    error = "Could not open the release page. Check that a browser is available and try again."
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Update Release")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
    )
}

/** Keep all visible highlights available to expansion and omit fenced verification blocks. */
internal fun cleanReleaseHighlights(raw: String): List<String> {
    var inFence = false
    val lines = raw.lineSequence().mapNotNull { line ->
        val text = line.trim()
        if (text.startsWith("```") || text.startsWith("~~~")) {
            inFence = !inFence
            null
        } else if (inFence || text.isBlank() || text.startsWith('#') || text.startsWith("http")) null
        else text
    }.toList()
    val bullets = lines.filter { it.startsWith("- ") || it.startsWith("* ") || Regex("^[0-9]+[.)] .*" ).matches(it) }
    return (bullets.ifEmpty { lines }).map {
        it.replace(Regex("^(?:[-*] |[0-9]+[.)] )"), "")
            .replace("**", "").replace("__", "").trim()
    }.filter { it.isNotBlank() }.distinct().take(40)
        .ifEmpty { listOf("See the release page for the latest improvements.") }
}
