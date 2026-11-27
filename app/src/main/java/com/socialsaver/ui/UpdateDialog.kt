package com.socialsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.socialsaver.BuildConfig
import com.socialsaver.MainViewModel
import com.socialsaver.UpdateState
import com.socialsaver.core.formatBytes
import com.socialsaver.data.Updater

/** "A new version is ready" popup: Update now opens the download link, Later closes it. */
@Composable
fun UpdateDialog(vm: MainViewModel, u: UpdateState) {
    val info = u.info ?: return
    if (!u.showDialog) return
    val ctx = LocalContext.current

    AlertDialog(
        onDismissRequest = vm::dismissUpdate,
        title = { Text("Update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val size = formatBytes(info.sizeBytes)?.let { " ($it)" }.orEmpty()
                Text("Version ${info.versionName} is ready$size. You have ${BuildConfig.VERSION_NAME}.")
                Text(
                    "Your browser will download it. Open the downloaded file and tap Update. Your saved list stays as it is.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { Updater.openPage(ctx, info.apkUrl); vm.dismissUpdate() }) { Text("Update now") }
        },
        dismissButton = {
            TextButton(onClick = vm::dismissUpdate) { Text("Later") }
        },
    )
}
