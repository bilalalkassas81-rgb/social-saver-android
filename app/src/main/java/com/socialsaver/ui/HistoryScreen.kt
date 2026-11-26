package com.socialsaver.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.socialsaver.MainViewModel
import com.socialsaver.core.HistoryItem
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val items by vm.history.items.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        items(items, key = { it.id }) { item ->
            HistoryRow(
                item = item,
                onOpen = { item.uris.firstOrNull()?.let { openMedia(ctx, it, item.mimes.firstOrNull() ?: "*/*") } },
                onShare = { shareMedia(ctx, item.uris, item.mimes) },
                onDelete = { vm.removeHistory(item.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(item: HistoryItem, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val date = remember(item.timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.timestamp))
    }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val files = if (item.uris.size > 1) " · ${item.uris.size} files" else ""
                Text(
                    "${item.platform} · ${item.quality}$files · $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove from history") }
        }
    }
}
