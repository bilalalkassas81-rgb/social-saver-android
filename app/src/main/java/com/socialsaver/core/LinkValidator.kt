package com.socialsaver.core

import java.net.URI

sealed class LinkCheck {
    data class Valid(val url: String, val platform: Platform) : LinkCheck()
    data class Invalid(val reason: String) : LinkCheck()
}

object LinkValidator {
    private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    private val domains = mapOf(
        Platform.INSTAGRAM to listOf("instagram.com", "instagr.am"),
        Platform.FACEBOOK to listOf("facebook.com", "fb.watch", "fb.com", "fb.me"),
        Platform.X to listOf("x.com", "twitter.com", "t.co"),
    )

    /** Pulls the first link out of pasted text like "Look at this https://... !" */
    fun extractUrl(text: String): String? =
        urlRegex.find(text.trim())?.value?.trimEnd('.', ',', ')', ']', '!', ';')

    fun hostOf(url: String): String? =
        try { URI(url).host?.lowercase() } catch (e: Exception) { null }

    fun platformOf(url: String): Platform {
        val host = hostOf(url) ?: return Platform.OTHER
        for ((platform, list) in domains) {
            if (list.any { host == it || host.endsWith(".$it") }) return platform
        }
        return Platform.OTHER
    }

    fun check(text: String): LinkCheck {
        if (text.isBlank()) return LinkCheck.Invalid("Paste a link first.")
        val url = extractUrl(text)
            ?: return LinkCheck.Invalid("That doesn't look like a link. It should start with https://")
        val host = hostOf(url)
        if (host.isNullOrBlank() || !host.contains('.')) {
            return LinkCheck.Invalid("That link looks incomplete.")
        }
        return LinkCheck.Valid(url, platformOf(url))
    }
}
