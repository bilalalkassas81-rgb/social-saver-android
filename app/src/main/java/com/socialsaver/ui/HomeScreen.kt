package com.socialsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.socialsaver.MainViewModel
import com.socialsaver.Phase
import com.socialsaver.core.MediaInfo
import com.socialsaver.core.QualityOption
import com.socialsaver.core.formatBytes

@Composable
fun HomeScreen(vm: MainViewModel) {
    val s by vm.home.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Social Saver", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Paste an Instagram, Facebook or X link.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = s.input,
            onValueChange = vm::onInput,
            modifier = Modifier.fillMaxWidth(),
            enabled = !s.busy,
            singleLine = true,
            placeholder = { Text("https://…") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.fetch() }),
            trailingIcon = {
                TextButton(onClick = { clipboard.getText()?.text?.let(vm::onInput) }, enabled = !s.busy) {
                    Text("Paste")
                }
            },
        )

        Button(
            onClick = vm::fetch,
            enabled = !s.busy && s.input.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (s.phase == Phase.FETCHING) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                Spacer(Modifier.width(8.dp))
                Text("Fetching…")
            } else {
                Text("Fetch")
            }
        }

        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        s.info?.let { info ->
            MediaCard(info)
            QualityPicker(info.options, s.selected, enabled = !s.busy, onSelect = vm::select)

            when (s.phase) {
                Phase.DOWNLOADING -> {
                    val p = s.progress
                    if (p == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                        Text("${(p * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = vm::cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
                Phase.DONE -> {
                    s.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { s.saved.firstOrNull()?.let { openMedia(ctx, it.uri, it.mime) } },
                            modifier = Modifier.weight(1f),
                        ) { Text("Open") }
                        OutlinedButton(onClick = vm::newLink, modifier = Modifier.weight(1f)) { Text("New link") }
                    }
                }
                else -> Button(
                    onClick = vm::download,
                    enabled = info.options.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Download") }
            }
        }
    }
}

@Composable
private fun MediaCard(info: MediaInfo) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = info.thumbnail,
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(info.title, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val extra = if (info.itemCount > 1) " · ${info.itemCount} items" else ""
                Text(
                    info.platform.label + extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QualityPicker(options: List<QualityOption>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text("Quality", style = MaterialTheme.typography.titleSmall)
        options.forEachIndexed { i, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = i == selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = i == selected, onClick = null, enabled = enabled)
                Spacer(Modifier.width(8.dp))
                Text(option.label, modifier = Modifier.weight(1f))
                formatBytes(option.sizeBytes)?.let {
                    Text("~$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
