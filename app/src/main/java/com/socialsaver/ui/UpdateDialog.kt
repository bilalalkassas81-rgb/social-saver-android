package com.socialsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.socialsaver.BuildConfig
import com.socialsaver.MainViewModel
import com.socialsaver.UpdateState
import com.socialsaver.core.formatBytes
import com.socialsaver.data.Updater

/** "A new version is ready" popup: Update now / Later, like any app. */
@Composable
fun UpdateDialog(vm: MainViewModel, u: UpdateState) {
    val info = u.info ?: return
    if (!u.showDialog) return
    val ctx = LocalContext.current
    var needsPermission by remember { mutableStateOf(false) }

    // As soon as the file is downloaded, open Android's installer.
    LaunchedEffect(u.apk) {
        u.apk?.let { needsPermission = !Updater.install(ctx, it) }
    }

    AlertDialog(
        onDismissRequest = { if (!u.downloading) vm.dismissUpdate() },
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    u.error != null -> Text(u.error)
                    u.downloading -> {
                        val p = u.progress
                        Text(if (p == null) "Downloading…" else "Downloading… ${(p * 100).toInt()}%")
                        val bar = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                        if (p == null) {
                            LinearProgressIndicator(bar, color = MaterialTheme.colorScheme.tertiary)
                        } else {
                            LinearProgressIndicator(progress = { p }, modifier = bar, color = MaterialTheme.colorScheme.tertiary, drawStopIndicator = {})
                        }
                    }
                    u.apk != null && needsPermission -> Text(
                        "Android needs your OK first: turn on \"Allow from this source\" for Social Saver, come back, and tap Install."
                    )
                    u.apk != null -> Text("Ready. Follow Android's steps to finish the update.")
                    else -> {
                        val size = formatBytes(info.sizeBytes)?.let { " ($it)" }.orEmpty()
                        Text("Version ${info.versionName} is ready$size. You have ${BuildConfig.VERSION_NAME}.")
                        Text(
                            "Your saved list stays as it is.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                u.error != null -> Button(onClick = { Updater.openPage(ctx, info.pageUrl); vm.dismissUpdate() }) {
                    Text("Open download page")
                }
                u.downloading -> Button(onClick = {}, enabled = false) { Text("Updating…") }
                u.apk != null -> Button(onClick = { u.apk?.let { needsPermission = !Updater.install(ctx, it) } }) { Text("Install") }
                else -> Button(onClick = vm::startUpdate) { Text("Update now") }
            }
        },
        dismissButton = {
            if (!u.downloading) TextButton(onClick = vm::dismissUpdate) { Text("Later") }
        },
    )
}
