package com.socialsaver.core

import org.json.JSONObject
import java.math.BigInteger

/** Instagram's web endpoints, the same ones instagram.com uses in a browser. Pure parsing, no network. */
object InstagramParsing {
    const val APP_ID = "936619743392459"
    const val ASBD_ID = "359341"
    const val GRAPHQL_DOC_ID = "27130156389949648"
    const val GRAPHQL_NAME = "PolarisLoggedOutDesktopWWWPostRootContentQuery"

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val codeRegex = Regex("""instagram\.com/(?:[^/?#]+/)?(?:p|reels?|tv)/([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    private val lsdPatterns = listOf(
        Regex(""""LSD",\[],\{"token":"([^"]+)""""),
        Regex(""""lsd":"([^"]+)""""),
        Regex("""name="lsd" value="([^"]+)""""),
        Regex("""<script[^>]*id="__eqmc"[^>]*>\s*\{.*?"l":"([^"]+)"""", RegexOption.DOT_MATCHES_ALL),
    )

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

    /** The anti-forgery token instagram.com puts in its HTML for logged-out requests. */
    fun extractLsd(html: String): String? =
        lsdPatterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1)?.takeIf { t -> t.isNotBlank() } }

    data class IgVideo(val url: String, val width: Int?, val height: Int?)

    data class IgPost(
        val title: String,
        val thumbnail: String?,
        /** Best file of every item (one for a single post, several for a carousel). */
        val items: List<DirectItem>,
        /** Every size Instagram offers, only for a single-video post. */
        val videoVersions: List<IgVideo> = emptyList(),
    )

    /** Parses https://www.instagram.com/api/v1/media/<id>/info/ (logged in). */
    fun parseMediaInfo(json: String): IgPost? =
        JSONObject(json).optJSONArray("items")?.optJSONObject(0)?.let(::parseProduct)

    /** Parses the logged-out GraphQL answer: data.xig_polaris_media.if_not_gated_logged_out */
    fun parseGraphql(json: String): IgPost? {
        val start = json.indexOf('{')
        if (start < 0) return null
        val product = JSONObject(json.substring(start))
            .optJSONObject("data")
            ?.optJSONObject("xig_polaris_media")
            ?.optJSONObject("if_not_gated_logged_out")
            ?: return null
        return parseProduct(product)
    }

    fun parseProduct(item: JSONObject): IgPost? {
        val nodes = item.optJSONArray("carousel_media")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        }?.takeIf { it.isNotEmpty() } ?: listOf(item)

        val items = nodes.mapNotNull { node ->
            videos(node).firstOrNull()?.let { DirectItem(it.url, true) }
                ?: bestCandidate(node)?.let { DirectItem(it, false) }
        }
        if (items.isEmpty()) return null

        val user = item.optJSONObject("user")?.str("username")
            ?: item.optJSONObject("owner")?.str("username")
        val caption = item.optJSONObject("caption")?.str("text")
        val title = caption?.let { tidyTitle(it) } ?: user?.let { "Post by $it" } ?: "Instagram post"
        val versions = if (nodes.size == 1) videos(nodes.first()) else emptyList()
        return IgPost(title, bestCandidate(nodes.first()), items, versions)
    }

    /** Video sizes, biggest first, duplicates removed. */
    private fun videos(node: JSONObject): List<IgVideo> {
        val arr = node.optJSONArray("video_versions") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .mapNotNull { v -> v.str("url")?.let { IgVideo(it, v.num("width")?.toInt(), v.num("height")?.toInt()) } }
            .sortedByDescending { (it.width ?: 0) * (it.height ?: 0) }
            .distinctBy { it.url }
    }

    private fun bestCandidate(node: JSONObject): String? {
        val arr = node.optJSONObject("image_versions2")?.optJSONArray("candidates") ?: return null
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
            .maxByOrNull { (it.num("width") ?: 0.0) * (it.num("height") ?: 0.0) }
            ?.str("url")
    }

    /** The choices shown to the user for a post we fetched ourselves. */
    fun options(post: IgPost): List<QualityOption> {
        if (post.items.size == 1 && post.items.first().isVideo && post.videoVersions.isNotEmpty()) {
            val seen = mutableSetOf<String>()
            val sized = post.videoVersions.mapNotNull { v ->
                val short = listOfNotNull(v.width, v.height).filter { it > 0 }.minOrNull() ?: return@mapNotNull null
                val label = "${short}p"
                if (seen.add(label)) QualityOption(label, "", OptionKind.DIRECT, items = listOf(DirectItem(v.url, true))) else null
            }
            if (sized.isNotEmpty()) return sized
            return listOf(QualityOption("Best quality", "", OptionKind.DIRECT, items = post.items))
        }
        val videos = post.items.count { it.isVideo }
        val photos = post.items.size - videos
        val label = when {
            post.items.size == 1 && videos == 1 -> "Best quality"
            post.items.size == 1 -> "Photo, full size"
            videos == 0 -> "All $photos photos"
            photos == 0 -> "All $videos videos"
            else -> "All ${post.items.size} items"
        }
        return listOf(QualityOption(label, "", OptionKind.DIRECT, items = post.items))
    }

    /** Form body for the logged-out GraphQL request (what instagram.com itself sends). */
    fun graphqlForm(lsd: String, mediaId: String): Map<String, String> = linkedMapOf(
        "lsd" to lsd,
        "fb_api_caller_class" to "RelayModern",
        "fb_api_req_friendly_name" to GRAPHQL_NAME,
        "server_timestamps" to "true",
        "variables" to """{"media_id":"$mediaId"}""",
        "doc_id" to GRAPHQL_DOC_ID,
    )
}
