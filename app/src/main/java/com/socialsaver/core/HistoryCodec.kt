package com.socialsaver.core

import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: Long,
    val title: String,
    val platform: String,
    val sourceUrl: String,
    val thumbnail: String?,
    val quality: String,
    val uris: List<String>,
    val mimes: List<String>,
    val timestamp: Long,
)

object HistoryCodec {
    fun encode(items: List<HistoryItem>): String {
        val arr = JSONArray()
        for (it in items) {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("platform", it.platform)
                    .put("sourceUrl", it.sourceUrl)
                    .put("thumbnail", it.thumbnail ?: JSONObject.NULL)
                    .put("quality", it.quality)
                    .put("uris", JSONArray(it.uris))
                    .put("mimes", JSONArray(it.mimes))
                    .put("timestamp", it.timestamp)
            )
        }
        return arr.toString()
    }

    fun decode(text: String): List<HistoryItem> = try {
        val arr = JSONArray(text)
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.let(::itemOf) }
    } catch (e: Exception) {
        emptyList()
    }

    private fun itemOf(o: JSONObject): HistoryItem? {
        val uris = strings(o.optJSONArray("uris"))
        if (uris.isEmpty()) return null
        return HistoryItem(
            id = o.optLong("id"),
            title = o.str("title") ?: "Download",
            platform = o.str("platform") ?: "",
            sourceUrl = o.str("sourceUrl") ?: "",
            thumbnail = o.str("thumbnail"),
            quality = o.str("quality") ?: "",
            uris = uris,
            mimes = strings(o.optJSONArray("mimes")),
            timestamp = o.optLong("timestamp"),
        )
    }

    private fun strings(arr: JSONArray?): List<String> =
        (0 until (arr?.length() ?: 0)).mapNotNull { arr!!.optString(it).takeIf { s -> s.isNotBlank() } }
}
