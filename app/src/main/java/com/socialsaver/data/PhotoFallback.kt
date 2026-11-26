package com.socialsaver.data

import com.socialsaver.core.MediaInfo
import com.socialsaver.core.OptionKind
import com.socialsaver.core.PhotoParsing
import com.socialsaver.core.Platform
import com.socialsaver.core.QualityOption
import java.io.File

/** Used when yt-dlp finds no video: photo posts on X, Instagram and Facebook. */
object PhotoFallback {

    /** Blocking; call from a background thread. Returns null when nothing is found. */
    fun find(url: String, platform: Platform): MediaInfo? = try {
        if (platform == Platform.X) fromTwitter(url) else fromOpenGraph(url, platform)
    } catch (e: Exception) {
        null
    }

    private fun fromTwitter(url: String): MediaInfo? {
        val id = PhotoParsing.tweetId(url) ?: return null
        val result = PhotoParsing.parseFxTwitter(
            Http.getText("https://api.fxtwitter.com/status/$id", Http.BROWSER_UA)
        )
        if (result.photos.isEmpty()) return null
        return photoInfo(url, Platform.X, result.title, result.photos)
    }

    private fun fromOpenGraph(url: String, platform: Platform): MediaInfo? {
        val html = Http.getText(url, Http.PREVIEW_UA)
        val images = PhotoParsing.ogImages(html)
        if (images.isEmpty()) return null
        return photoInfo(url, platform, PhotoParsing.ogTitle(html), images)
    }

    private fun photoInfo(url: String, platform: Platform, title: String?, photos: List<String>) = MediaInfo(
        url = url,
        title = title ?: "${platform.label} photo",
        thumbnail = photos.first(),
        platform = platform,
        options = listOf(
            QualityOption(
                if (photos.size == 1) "Photo (best available)" else "${photos.size} photos (best available)",
                "",
                OptionKind.PHOTOS,
            )
        ),
        itemCount = photos.size,
        photoUrls = photos,
    )

    /** Downloads one photo into [dir]; the file extension comes from the server's content type. */
    fun downloadPhoto(url: String, dir: File, baseName: String): File {
        val tmp = File(dir, "$baseName.tmp")
        val type = Http.download(url, tmp)?.lowercase().orEmpty()
        val ext = when {
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
