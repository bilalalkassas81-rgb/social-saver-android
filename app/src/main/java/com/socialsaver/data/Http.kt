package com.socialsaver.data

import com.socialsaver.core.CookieFile
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Http {
    const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    /** Social sites serve link-preview tags (og:image) to this crawler without a login. */
    const val PREVIEW_UA = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    /** Cookies kept between requests, like a browser tab. */
    class Session {
        val cookies = linkedMapOf<String, String>()
        fun header(): String = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    data class Response(val code: Int, val body: String, val finalUrl: String)

    private fun open(url: String, ua: String, headers: Map<String, String> = emptyMap()): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            headers.forEach { (k, v) -> if (v.isNotEmpty()) setRequestProperty(k, v) }
        }

    /** GET or form POST that remembers cookies in [session] and never throws on HTTP errors. */
    fun request(
        url: String,
        ua: String,
        headers: Map<String, String> = emptyMap(),
        form: Map<String, String>? = null,
        session: Session? = null,
    ): Response {
        val all = if (session != null && session.cookies.isNotEmpty()) headers + ("Cookie" to session.header()) else headers
        val c = open(url, ua, all)
        try {
            if (form != null) {
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val body = form.entries.joinToString("&") {
                    URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
                }
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            if (session != null) {
                c.headerFields.entries
                    .filter { it.key?.equals("Set-Cookie", ignoreCase = true) == true }
                    .flatMap { it.value }
                    .mapNotNull(CookieFile::parseSetCookie)
                    .forEach { (k, v) -> session.cookies[k] = v }
            }
            val stream = if (code >= 400) c.errorStream else c.inputStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            return Response(code, text, c.url.toString())
        } finally {
            c.disconnect()
        }
    }

    fun getText(url: String, ua: String, headers: Map<String, String> = emptyMap()): String {
        val r = request(url, ua, headers)
        if (r.code !in 200..299) throw IOException("HTTP ${r.code}")
        return r.body
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
