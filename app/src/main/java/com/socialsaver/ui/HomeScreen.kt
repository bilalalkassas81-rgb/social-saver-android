@file:OptIn(ExperimentalMaterial3Api::class)

package com.socialsaver.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.socialsaver.HomeState
import com.socialsaver.MainViewModel
import com.socialsaver.Phase
import com.socialsaver.core.MediaInfo
import com.socialsaver.core.OptionKind
import com.socialsaver.core.Platform
import com.socialsaver.core.QualityOption
import com.socialsaver.core.formatBytes

@Composable
fun HomeScreen(vm: MainViewModel, onLogin: (Platform) -> Unit) {
    val s by vm.home.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Social Saver", style = MaterialTheme.typography.titleLarge)
        LinkPanel(s, vm)
        s.error?.let { ErrorNote(it, s.loginFor, onLogin) }
        s.info?.let { info ->
            MediaPreview(info)
            QualityPills(info.options, s.selected, enabled = !s.busy, onSelect = vm::select)
            ActionArea(s, info, vm)
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** The hero: one bold indigo panel where the link goes. */
@Composable
private fun LinkPanel(s: HomeState, vm: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    val c = MaterialTheme.colorScheme
    Surface(color = c.primary, contentColor = c.onPrimary, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Save it.", style = MaterialTheme.typography.displaySmall)
            Text(
                "Paste a link from Instagram, Facebook or X.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.onPrimary.copy(alpha = 0.78f),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.onPrimary.copy(alpha = 0.14f))
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = s.input,
                    onValueChange = vm::onInput,
                    enabled = !s.busy,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.onPrimary),
                    cursorBrush = SolidColor(c.onPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.fetch() }),
                    modifier = Modifier.weight(1f).padding(vertical = 16.dp),
                    decorationBox = { inner ->
                        Box {
                            if (s.input.isEmpty()) {
                                Text("https://…", style = MaterialTheme.typography.bodyLarge, color = c.onPrimary.copy(alpha = 0.5f))
                            }
                            inner()
                        }
                    },
                )
                if (s.input.isEmpty()) {
                    TextButton(
                        onClick = { clipboard.getText()?.text?.let(vm::onInput) },
                        colors = ButtonDefaults.textButtonColors(contentColor = c.onPrimary),
                    ) { Text("Paste") }
                } else {
                    IconButton(onClick = { vm.onInput("") }, enabled = !s.busy) {
                        Icon(Icons.Default.Close, contentDescription = "Clear link", tint = c.onPrimary.copy(alpha = 0.8f))
                    }
                }
            }
            Button(
                onClick = vm::fetch,
                enabled = !s.busy && s.input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.onPrimary,
                    contentColor = c.primary,
                    disabledContainerColor = c.onPrimary.copy(alpha = 0.22f),
                    disabledContentColor = c.onPrimary.copy(alpha = 0.85f),
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (s.phase == Phase.FETCHING) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                    Spacer(Modifier.width(10.dp))
                    Text("Looking at the post…")
                } else {
                    Text("Find post")
                }
            }
        }
    }
}

@Composable
private fun ErrorNote(message: String, loginFor: Platform?, onLogin: (Platform) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(color = c.errorContainer, contentColor = c.onErrorContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (loginFor != null) {
                Button(
                    onClick = { onLogin(loginFor) },
                    colors = ButtonDefaults.buttonColors(containerColor = c.onErrorContainer, contentColor = c.errorContainer),
                ) { Text("Log in to ${loginFor.label}") }
            }
        }
    }
}

@Composable
private fun MediaPreview(info: MediaInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp))) {
            val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = info.thumbnail,
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            PlatformTag(info.platform, info.platform.label, Modifier.align(Alignment.TopStart).padding(12.dp))
            if (info.itemCount > 1) {
                PlatformTag(null, "${info.itemCount} items", Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
        }
        Text(info.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QualityPills(options: List<QualityOption>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, option ->
            val on = i == selected
            Surface(
                onClick = { onSelect(i) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = if (on) c.primary else c.surface,
                contentColor = if (on) c.onPrimary else c.onSurface,
                border = if (on) null else BorderStroke(1.dp, c.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(option.label, style = MaterialTheme.typography.labelLarge)
                    formatBytes(option.sizeBytes)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = LocalContentColor.current.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionArea(s: HomeState, info: MediaInfo, vm: MainViewModel) {
    val c = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    when (s.phase) {
        Phase.DOWNLOADING -> Surface(color = c.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val p = s.progress
                    Text(if (p == null) "Starting…" else "${(p * 100).toInt()}%", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = vm::cancel) { Text("Cancel") }
                }
                val barModifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                val p = s.progress
                if (p == null) {
                    LinearProgressIndicator(barModifier, color = c.tertiary, trackColor = c.tertiary.copy(alpha = 0.18f))
                } else {
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = barModifier,
                        color = c.tertiary,
                        trackColor = c.tertiary.copy(alpha = 0.18f),
                        drawStopIndicator = {},
                    )
                }
            }
        }

        Phase.DONE -> Surface(color = c.primaryContainer, contentColor = c.onPrimaryContainer, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(s.message.orEmpty(), style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { s.saved.firstOrNull()?.let { openMedia(ctx, it.uri, it.mime) } },
                        modifier = Modifier.weight(1f),
                    ) { Text("Open") }
                    FilledTonalButton(
                        onClick = { shareMedia(ctx, s.saved.map { it.uri }, s.saved.map { it.mime }) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Share") }
                }
                TextButton(onClick = vm::newLink) { Text("Save another link") }
            }
        }

        else -> {
            val option = info.options.getOrNull(s.selected)
            Button(
                onClick = vm::download,
                enabled = option != null,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = c.tertiary, contentColor = c.onTertiary),
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Text(
                    when (option?.kind) {
                        OptionKind.AUDIO -> "Download MP3"
                        OptionKind.VIDEO -> "Download ${option.label}"
                        else -> "Download"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
