package com.socialsaver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.socialsaver.MainViewModel
import com.socialsaver.core.HistoryItem
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel, onGoHome: () -> Unit) {
    val items by vm.history.items.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    if (items.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Nothing saved yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Videos and photos you download show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onGoHome) { Text("Paste a link") }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("Your saves", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.weight(1f))
                Text(
                    "${items.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(items, key = { it.id }) { item ->
            HistoryTile(
                item = item,
                onOpen = { item.uris.firstOrNull()?.let { openMedia(ctx, it, item.mimes.firstOrNull() ?: "*/*") } },
                onShare = { shareMedia(ctx, item.uris, item.mimes) },
                onRemove = { vm.removeHistory(item.id) },
            )
        }
    }
}

@Composable
private fun HistoryTile(item: HistoryItem, onOpen: () -> Unit, onShare: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val date = remember(item.timestamp) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.timestamp)) }
    val c = MaterialTheme.colorScheme

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onOpen),
        ) {
            val placeholder = ColorPainter(c.surfaceVariant)
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val files = if (item.uris.size > 1) "${item.platform}, ${item.uris.size} files" else item.platform
            PlatformTag(platformByLabel(item.platform), files, Modifier.align(Alignment.BottomStart).padding(8.dp))
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp)) {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.size(34.dp).background(c.surface.copy(alpha = 0.92f), CircleShape),
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp), tint = c.onSurface)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = { menu = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from list") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menu = false; onRemove() },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            "${item.quality}, $date",
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
