package com.socialsaver.data

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Http {
    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    /** Social sites serve link-preview tags (og:image) to this crawler without a login. */
    const val PREVIEW_UA = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    private fun open(url: String, ua: String, headers: Map<String, String> = emptyMap()): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            headers.forEach { (k, v) -> if (v.isNotEmpty()) setRequestProperty(k, v) }
        }

    fun getText(url: String, ua: String, headers: Map<String, String> = emptyMap()): String {
        val c = open(url, ua, headers)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Downloads to [dest] and returns the Content-Type header. */
    fun download(url: String, dest: File, ua: String = BROWSER_UA): String? {
        val c = open(url, ua)
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            c.inputStream.use { input -> dest.outputStream().use { input.copyTo(it) } }
            return c.contentType
        } finally {
            c.disconnect()
        }
    }
}
