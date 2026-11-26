package com.socialsaver.core

import org.json.JSONObject
import java.math.BigInteger

/** Instagram's own media API (used when logged in). Pure parsing, no network. */
object InstagramParsing {
    const val APP_ID = "936619743392459"
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val codeRegex = Regex("""instagram\.com/(?:[^/?#]+/)?(?:p|reels?|tv)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)

    fun shortcode(url: String): String? = codeRegex.find(url)?.groupValues?.get(1)

    /** Shortcode -> numeric media id, the same way Instagram's web app does it. */
    fun mediaId(shortcode: String): String {
        val code = if (shortcode.length > 28) shortcode.dropLast(28) else shortcode
        var n = BigInteger.ZERO
        val base = BigInteger.valueOf(64)
        for (c in code) {
            val digit = ALPHABET.indexOf(c)
            require(digit >= 0) { "Bad shortcode" }
            n = n.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        return n.toString()
    }

    data class IgPost(val title: String, val thumbnail: String?, val items: List<DirectItem>)

    /** Parses https://www.instagram.com/api/v1/media/<id>/info/ */
    fun parseMediaInfo(json: String): IgPost? {
        val item = JSONObject(json).optJSONArray("items")?.optJSONObject(0) ?: return null
        val nodes = item.optJSONArray("carousel_media")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        } ?: listOf(item)

        val items = nodes.mapNotNull { node ->
            bestUrl(node, "video_versions")?.let { DirectItem(it, true) }
                ?: bestCandidate(node)?.let { DirectItem(it, false) }
        }
        if (items.isEmpty()) return null

        val user = item.optJSONObject("user")?.str("username")
        val caption = item.optJSONObject("caption")?.str("text")
        val title = caption?.let { tidyTitle(it) } ?: user?.let { "Post by $it" } ?: "Instagram post"
        return IgPost(title, bestCandidate(nodes.first()), items)
    }

    private fun bestUrl(node: JSONObject, key: String): String? {
        val arr = node.optJSONArray(key) ?: return null
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .maxByOrNull { (it.num("width") ?: 0.0) * (it.num("height") ?: 0.0) }
            ?.str("url")
    }

    private fun bestCandidate(node: JSONObject): String? =
        node.optJSONObject("image_versions2")?.let { bestUrl(it, "candidates") }
}
