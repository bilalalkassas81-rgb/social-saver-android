package com.socialsaver.core

import java.util.Locale

enum class Platform(val label: String) {
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    X("X"),
    OTHER("Web"),
}

enum class OptionKind { VIDEO, AUDIO, DIRECT }

/** A file we can fetch directly by URL (photo posts, Instagram API, fxtwitter). */
data class DirectItem(val url: String, val isVideo: Boolean)

data class QualityOption(
    val label: String,
    val selector: String,
    val kind: OptionKind,
    val sizeBytes: Long? = null,
)

data class MediaInfo(
    val url: String,
    val title: String,
    val thumbnail: String?,
    val platform: Platform,
    val options: List<QualityOption>,
    val itemCount: Int = 1,
    val directItems: List<DirectItem> = emptyList(),
)

/** One entry of yt-dlp's "formats" list, reduced to what we need. */
data class RawFormat(
    val formatId: String?,
    val width: Int?,
    val height: Int?,
    val vcodec: String?,
    val acodec: String?,
    val filesize: Long?,
)

/** Shown to the user as-is. [loginFor] means "logging in to this site would fix it". */
class UserFacingException(message: String, val loginFor: Platform? = null) : Exception(message)

fun formatBytes(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
    else "${(bytes / 1024).coerceAtLeast(1)} KB"
}

fun tidyTitle(raw: String, max: Int = 120): String {
    val t = raw.replace(Regex("\\s+"), " ").trim()
    return if (t.length <= max) t else t.take(max - 1).trimEnd() + "…"
}
