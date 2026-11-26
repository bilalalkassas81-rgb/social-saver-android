package com.socialsaver.core

import java.util.Locale

enum class Platform(val label: String) {
    INSTAGRAM("Instagram"),
    FACEBOOK("Facebook"),
    X("X"),
    OTHER("Web"),
}

enum class OptionKind { VIDEO, AUDIO, PHOTOS }

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
    val photoUrls: List<String> = emptyList(),
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

/** Shown to the user as-is. */
class UserFacingException(message: String) : Exception(message)

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
