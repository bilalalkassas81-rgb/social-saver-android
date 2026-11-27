package com.socialsaver.data

import com.socialsaver.core.CookieFile
import com.socialsaver.core.DirectItem
import com.socialsaver.core.InstagramParsing
import com.socialsaver.core.MediaInfo
import com.socialsaver.core.OptionKind
import com.socialsaver.core.PhotoParsing
import com.socialsaver.core.Platform
import com.socialsaver.core.QualityOption
import java.io.File

/** Used when yt-dlp can't get the post. Blocking: call from a background thread. */
object Fallbacks {

    /** Instagram's own API with the logged-in session: videos, photos and carousels in original quality. */
    fun instagram(url: String, cookieHeader: String): MediaInfo? {
        val code = InstagramParsing.shortcode(url) ?: return null
        val csrf = CookieFile.parseHeader(cookieHeader).firstOrNull { it.first == "csrftoken" }?.second.orEmpty()
        val json = Http.getText(
            "https://www.instagram.com/api/v1/media/${InstagramParsing.mediaId(code)}/info/",
            Http.BROWSER_UA,
            mapOf(
                "Cookie" to cookieHeader,
                "X-IG-App-ID" to InstagramParsing.APP_ID,
                "X-CSRFToken" to csrf,
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to "https://www.instagram.com/",
            ),
        )
        return InstagramParsing.parseMediaInfo(json)?.let { fromIgPost(url, it) }
    }

    fun fromIgPost(url: String, post: InstagramParsing.IgPost) = MediaInfo(
        url = url,
        title = post.title,
        thumbnail = post.thumbnail ?: post.items.firstOrNull { !it.isVideo }?.url,
        platform = Platform.INSTAGRAM,
        options = InstagramParsing.options(post),
        itemCount = post.items.size,
        directItems = post.items,
    )

    /** X photos and videos through the public fxtwitter API (no login needed). */
    fun twitter(url: String): MediaInfo? {
        val id = PhotoParsing.tweetId(url) ?: return null
        val r = PhotoParsing.parseFxTwitter(Http.getText("https://api.fxtwitter.com/status/$id", Http.BROWSER_UA))
        if (r.items.isEmpty()) return null
        return directInfo(url, Platform.X, r.title, r.thumbnail, r.items)
    }

    /**
     * Link-preview photo for public photo posts. Returns null for video posts,
     * because their preview image is only the cover, not the content.
     */
    fun openGraphPhoto(url: String, platform: Platform): MediaInfo? {
        if (PhotoParsing.urlLooksLikeVideo(url)) return null
        val html = Http.getText(url, Http.PREVIEW_UA)
        if (PhotoParsing.isVideoPage(html)) return null
        val images = PhotoParsing.ogImages(html)
        if (images.isEmpty()) return null
        return directInfo(url, platform, PhotoParsing.ogTitle(html), images.first(), images.map { DirectItem(it, false) })
    }

    private fun directInfo(url: String, platform: Platform, title: String?, thumb: String?, items: List<DirectItem>): MediaInfo {
        val videos = items.count { it.isVideo }
        val photos = items.size - videos
        val label = when {
            items.size == 1 && videos == 1 -> "Best quality"
            items.size == 1 -> "Photo, full size"
            videos == 0 -> "All $photos photos"
            photos == 0 -> "All $videos videos"
            else -> "All ${items.size} items"
        }
        return MediaInfo(
            url = url,
            title = title ?: "${platform.label} post",
            thumbnail = thumb ?: items.firstOrNull { !it.isVideo }?.url,
            platform = platform,
            options = listOf(QualityOption(label, "", OptionKind.DIRECT, items = items)),
            itemCount = items.size,
            directItems = items,
        )
    }

    /** Downloads one item into [dir]; the extension comes from the server's content type. */
    fun downloadItem(item: DirectItem, dir: File, baseName: String): File {
        val tmp = File(dir, "$baseName.tmp")
        val type = Http.download(item.url, tmp)?.lowercase().orEmpty()
        val ext = when {
            item.isVideo || "video" in type -> "mp4"
            "png" in type -> "png"
            "webp" in type -> "webp"
            "heic" in type -> "heic"
            else -> "jpg"
        }
        val out = File(dir, "$baseName.$ext")
        tmp.renameTo(out)
        return out
    }
}
