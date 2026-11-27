package com.socialsaver.core

/** Converts browser cookies into the Netscape cookie file format yt-dlp reads. */
object CookieFile {
    const val HEADER = "# Netscape HTTP Cookie File"

    fun parseHeader(cookieHeader: String?): List<Pair<String, String>> =
        cookieHeader.orEmpty().split(';').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i).trim() to part.substring(i + 1).trim()
        }.filter { it.first.isNotEmpty() }

    /** "name=value; Path=/; ..." from a Set-Cookie header. Null for deletions. */
    fun parseSetCookie(header: String): Pair<String, String>? {
        val first = header.substringBefore(';')
        val i = first.indexOf('=')
        if (i <= 0) return null
        val name = first.substring(0, i).trim()
        val value = first.substring(i + 1).trim().removeSurrounding("\"")
        val lower = header.lowercase()
        if (value.isEmpty() || "max-age=0" in lower || "expires=thu, 01 jan 1970" in lower) return null
        return name to value
    }

    fun hasCookie(cookieHeader: String?, name: String): Boolean =
        parseHeader(cookieHeader).any { it.first == name && it.second.isNotEmpty() }

    /** One block of lines for a site, e.g. domain = ".instagram.com". */
    fun lines(domain: String, cookieHeader: String?, expiresEpochSeconds: Long): List<String> =
        parseHeader(cookieHeader).map { (name, value) ->
            listOf(domain, "TRUE", "/", "TRUE", expiresEpochSeconds.toString(), name, value).joinToString("\t")
        }

    fun build(blocks: List<List<String>>): String =
        (listOf(HEADER, "") + blocks.flatten()).joinToString("\n") + "\n"
}
