package com.socialsaver.core

import org.json.JSONObject

/** Pure parsing helpers for the photo fallback (no network here). */
object PhotoParsing {
    private val tweetRegex = Regex(
        """(?:twitter\.com|x\.com)/(?:[^/?#]+/status(?:es)?|i/web/status|i/status)/(\d+)""",
        RegexOption.IGNORE_CASE,
    )
    private val metaRegex = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val attrRegex = Regex("""([a-zA-Z_:][-\w:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)')""")

    fun tweetId(url: String): String? = tweetRegex.find(url)?.groupValues?.get(1)

    /** Ask Twitter's CDN for the original-size image. */
    fun twitterOriginal(url: String): String =
        if (url.contains("pbs.twimg.com/media/") && !url.contains("name=")) {
            url + (if ('?' in url) "&" else "?") + "name=orig"
        } else url

    data class FxResult(val title: String?, val photos: List<String>)

    /** Parses https://api.fxtwitter.com/status/<id> */
    fun parseFxTwitter(json: String): FxResult {
        val tweet = JSONObject(json).optJSONObject("tweet") ?: return FxResult(null, emptyList())
        val arr = tweet.optJSONObject("media")?.optJSONArray("photos")
        val photos = (0 until (arr?.length() ?: 0))
            .mapNotNull { arr!!.optJSONObject(it)?.str("url") }
            .map(::twitterOriginal)
            .distinct()
        val author = tweet.optJSONObject("author")?.str("name")
        val text = tweet.str("text")
        val title = listOfNotNull(author, text).joinToString(" - ").ifBlank { null }?.let { tidyTitle(it) }
        return FxResult(title, photos)
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

    fun unescape(s: String): String = s
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&#x2F;", "/")
        .replace("&amp;", "&")
}
