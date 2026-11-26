package com.socialsaver.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.socialsaver.BuildConfig
import com.socialsaver.MainViewModel
import com.socialsaver.core.Platform
import com.socialsaver.data.Account
import com.socialsaver.data.MediaSaver

@Composable
fun SettingsScreen(vm: MainViewModel, onLogin: (Platform) -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        Section(
            "Accounts",
            "Instagram and Facebook often only allow downloads when you're logged in. You log in on their own page, so your password never reaches this app.",
        ) {
            Account.entries.forEachIndexed { i, acc ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val loggedIn = acc in accounts
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(platformColor(acc.platform), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(acc.platform.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (loggedIn) "Logged in" else "Not logged in",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (loggedIn) MaterialTheme.colorScheme.primary else muted,
                        )
                    }
                    if (loggedIn) {
                        TextButton(onClick = { vm.logout(acc) }) { Text("Log out") }
                    } else {
                        Button(onClick = { onLogin(acc.platform) }) { Text("Log in") }
                    }
                }
            }
        }

        Section("Download engine", "Sites change often. If downloads stop working, update the engine.") {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("yt-dlp", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(s.engineVersion ?: "Starting…", style = MaterialTheme.typography.bodySmall, color = muted)
                }
                OutlinedButton(onClick = vm::updateEngine, enabled = !s.updating) {
                    Text(if (s.updating) "Updating…" else "Update")
                }
            }
            s.updateMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
            }
        }

        Section("Storage", null) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Save folder", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text("Downloads/${MediaSaver.FOLDER}", style = MaterialTheme.typography.bodySmall, color = muted)
                }
                TextButton(onClick = { confirmClear = true }) { Text("Clear history") }
            }
        }

        Text("Social Saver ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = muted)
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This only empties the list. Your downloaded files stay in Downloads.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Section(title: String, description: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
        }
    }
}
