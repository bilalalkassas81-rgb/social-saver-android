package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCodecTest {
    private val item = HistoryItem(
        id = 1L, title = "Reel – عربي ✓", platform = "Instagram", sourceUrl = "https://instagram.com/reel/1",
        thumbnail = null, quality = "720p", uris = listOf("content://media/1", "content://media/2"),
        mimes = listOf("video/mp4", "image/jpeg"), timestamp = 1_700_000_000_000,
    )

    @Test fun roundTrip() {
        val decoded = HistoryCodec.decode(HistoryCodec.encode(listOf(item, item.copy(id = 2, thumbnail = "https://t"))))
        assertEquals(listOf(item, item.copy(id = 2, thumbnail = "https://t")), decoded)
    }

    @Test fun corruptFileGivesEmptyList() {
        assertTrue(HistoryCodec.decode("{oops").isEmpty())
        assertTrue(HistoryCodec.decode("").isEmpty())
    }

    @Test fun skipsEntriesWithoutFiles() {
        val json = """[{"id":5,"title":"x","uris":[]},{"id":6,"title":"y","uris":["content://a"]}]"""
        assertEquals(listOf(6L), HistoryCodec.decode(json).map { it.id })
    }
}
