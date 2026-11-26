package com.socialsaver.data

import com.socialsaver.core.HistoryCodec
import com.socialsaver.core.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Download history kept in a small JSON file inside the app's private storage. */
class HistoryStore(private val file: File) {
    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()
    private val mutex = Mutex()
    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!loaded) {
                _items.value = if (file.exists()) HistoryCodec.decode(file.readText()) else emptyList()
                loaded = true
            }
        }
    }

    suspend fun add(item: HistoryItem) = change { listOf(item) + it }
    suspend fun remove(id: Long) = change { list -> list.filterNot { it.id == id } }
    suspend fun clear() = change { emptyList() }

    private suspend fun change(f: (List<HistoryItem>) -> List<HistoryItem>) = withContext(Dispatchers.IO) {
        load()
        mutex.withLock {
            val next = f(_items.value).take(500)
            _items.value = next
            file.writeText(HistoryCodec.encode(next))
        }
    }
}
