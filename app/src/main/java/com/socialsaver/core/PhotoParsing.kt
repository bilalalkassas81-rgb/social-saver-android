package com.socialsaver.core

import org.json.JSONObject

/** Pure parsing helpers for the fallbacks (no network here). */
object PhotoParsing {
    private val tweetRegex = Regex(
        """(?:twitter\.com|x\.com)/(?:[^/?#]+/status(?:es)?|i/web/status|i/status)/(\d+)""",
        RegexOption.IGNORE_CASE,
    )
    private val metaRegex = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val attrRegex = Regex("""([a-zA-Z_:][-\w:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val videoPathRegex = Regex(
        """instagram\.com/(?:[^/?#]+/)?(?:reels?|tv)/|facebook\.com/(?:watch|reel|.*/videos/)|fb\.watch/""",
        RegexOption.IGNORE_CASE,
    )

    fun tweetId(url: String): String? = tweetRegex.find(url)?.groupValues?.get(1)

    /** Reels, IGTV, Facebook watch/videos: these are videos, never "photo posts". */
    fun urlLooksLikeVideo(url: String): Boolean = videoPathRegex.containsMatchIn(url)

    /** Ask Twitter's CDN for the original-size image. */
    fun twitterOriginal(url: String): String =
        if (url.contains("pbs.twimg.com/media/") && !url.contains("name=")) {
            url + (if ('?' in url) "&" else "?") + "name=orig"
        } else url

    data class FxResult(val title: String?, val items: List<DirectItem>, val thumbnail: String?)

    /** Parses https://api.fxtwitter.com/status/<id> (photos and videos). */
    fun parseFxTwitter(json: String): FxResult {
        val tweet = JSONObject(json).optJSONObject("tweet") ?: return FxResult(null, emptyList(), null)
        val media = tweet.optJSONObject("media")
        val photos = media?.optJSONArray("photos")
        val videos = media?.optJSONArray("videos")
        val items = mutableListOf<DirectItem>()
        var thumb: String? = null
        for (i in 0 until (photos?.length() ?: 0)) {
            photos!!.optJSONObject(i)?.str("url")?.let {
                items += DirectItem(twitterOriginal(it), false)
                if (thumb == null) thumb = it
            }
        }
        for (i in 0 until (videos?.length() ?: 0)) {
            val v = videos!!.optJSONObject(i) ?: continue
            v.str("url")?.let { items += DirectItem(it, true) }
            if (thumb == null) thumb = v.str("thumbnail_url")
        }
        val author = tweet.optJSONObject("author")?.str("name")
        val text = tweet.str("text")
        val title = listOfNotNull(author, text).joinToString(" - ").ifBlank { null }?.let { tidyTitle(it) }
        return FxResult(title, items.distinctBy { it.url }, thumb)
    }

    fun metaTags(html: String): List<Map<String, String>> =
        metaRegex.findAll(html).map { tag ->
            attrRegex.findAll(tag.value).associate { m ->
                m.groupValues[1].lowercase() to unescape(m.groupValues[2].ifEmpty { m.groupValues[3] })
            }
        }.toList()

    private fun metaKey(tag: Map<String, String>) = (tag["property"] ?: tag["name"])?.lowercase()

    fun ogImages(html: String): List<String> =
        metaTags(html)
            .filter { metaKey(it) == "og:image" }
            .mapNotNull { it["content"] }
            .filter { it.startsWith("http") }
            .distinct()

    fun ogTitle(html: String): String? =
        metaTags(html).firstOrNull { metaKey(it) == "og:title" }?.get("content")
            ?.takeIf { it.isNotBlank() }?.let { tidyTitle(it) }

    /** True when the page describes a video, so its og:image is only a cover picture. */
    fun isVideoPage(html: String): Boolean = metaTags(html).any { tag ->
        val key = metaKey(tag) ?: return@any false
        key.startsWith("og:video") || (key == "og:type" && tag["content"].orEmpty().lowercase().startsWith("video"))
    }

    fun unescape(s: String): String = s
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&#x2F;", "/")
        .replace("&amp;", "&")
}
