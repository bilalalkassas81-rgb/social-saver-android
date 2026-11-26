package com.socialsaver.core

import org.json.JSONObject

/** Parses the output of `yt-dlp -J`. */
object InfoParser {
    fun parse(output: String, url: String, platform: Platform): MediaInfo {
        val start = output.indexOf('{')
        if (start < 0) throw UserFacingException("No video or photo was found in this post.")
        val root = JSONObject(output.substring(start))

        val entriesArr = root.optJSONArray("entries")
        val entries = (0 until (entriesArr?.length() ?: 0)).mapNotNull { entriesArr!!.optJSONObject(it) }
        val primary = entries.firstOrNull { it.has("formats") || it.has("url") } ?: root

        val title = listOf(root.str("title"), primary.str("title"), primary.str("description"))
            .firstOrNull { !it.isNullOrBlank() }
            ?.let { tidyTitle(it) } ?: "${platform.label} post"

        val thumbnail = primary.str("thumbnail") ?: root.str("thumbnail") ?: lastThumbnail(primary)

        return MediaInfo(
            url = url,
            title = title,
            thumbnail = thumbnail,
            platform = platform,
            options = QualityPlanner.plan(formatsOf(primary)),
            itemCount = if (entries.isEmpty()) 1 else entries.size,
        )
    }

    fun formatsOf(o: JSONObject): List<RawFormat> {
        val arr = o.optJSONArray("formats")
        if (arr == null || arr.length() == 0) {
            return if (o.has("url")) listOf(rawOf(o)) else emptyList()
        }
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map(::rawOf)
    }

    private fun rawOf(f: JSONObject) = RawFormat(
        formatId = f.str("format_id"),
        width = f.num("width")?.toInt(),
        height = f.num("height")?.toInt(),
        vcodec = f.str("vcodec"),
        acodec = f.str("acodec"),
        filesize = (f.num("filesize") ?: f.num("filesize_approx"))?.toLong(),
    )

    private fun lastThumbnail(o: JSONObject): String? {
        val arr = o.optJSONArray("thumbnails") ?: return null
        for (i in arr.length() - 1 downTo 0) arr.optJSONObject(i)?.str("url")?.let { return it }
        return null
    }
}

internal fun JSONObject.str(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

internal fun JSONObject.num(key: String): Double? =
    if (!has(key) || isNull(key)) null else optDouble(key, Double.NaN).takeIf { !it.isNaN() }
