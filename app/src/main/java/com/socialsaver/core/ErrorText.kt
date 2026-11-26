package com.socialsaver.core

/** Turns raw yt-dlp errors into short messages a normal person understands. */
object ErrorText {
    fun friendly(raw: String?): String {
        val text = raw.orEmpty()
        val lower = text.lowercase()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val line = lines.lastOrNull { it.startsWith("ERROR:") } ?: lines.lastOrNull().orEmpty()

        return when {
            listOf("login required", "rate-limit", "rate limit", "private", "cookies", "log in").any { it in lower } ->
                "This post needs a login, or the site is limiting requests. Version 1 works with public posts only. Try again in a few minutes."
            "unsupported url" in lower ->
                "This link isn't supported. Try a link to a single post or video."
            "no video" in lower ->
                "No video or photo was found in this post."
            listOf("unable to download", "network", "timed out", "failed to resolve", "connection").any { it in lower } ->
                "Network problem. Check your internet connection and try again."
            line.isBlank() -> "Something went wrong. Please try again."
            else -> line.removePrefix("ERROR:")
                .replace(Regex("""^\s*\[[^\]]+]\s*[^\s:]*:\s*"""), "")
                .trim().take(200)
        }
    }
}
