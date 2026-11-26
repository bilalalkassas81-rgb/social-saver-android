package com.socialsaver.data

import android.content.Context
import android.webkit.CookieManager
import com.socialsaver.core.CookieFile
import com.socialsaver.core.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Sites you can log in to inside the app. Passwords never touch our code: the site's own page handles them. */
enum class Account(
    val platform: Platform,
    val loginUrl: String,
    val siteUrl: String,
    val cookieDomain: String,
    val sessionCookie: String,
) {
    INSTAGRAM(Platform.INSTAGRAM, "https://www.instagram.com/accounts/login/", "https://www.instagram.com", ".instagram.com", "sessionid"),
    FACEBOOK(Platform.FACEBOOK, "https://m.facebook.com/login/", "https://www.facebook.com", ".facebook.com", "c_user");

    companion object {
        fun forPlatform(p: Platform): Account? = entries.firstOrNull { it.platform == p }
    }
}

/** Login sessions live in the system WebView cookie store, on this phone only. */
object Accounts {
    private const val ONE_YEAR = 365L * 24 * 3600

    /** CookieManager must be used on the main thread. */
    suspend fun cookieHeader(account: Account): String? = withContext(Dispatchers.Main) {
        CookieManager.getInstance().getCookie(account.siteUrl)
    }

    suspend fun isLoggedIn(account: Account): Boolean =
        CookieFile.hasCookie(cookieHeader(account), account.sessionCookie)

    suspend fun loggedIn(): Set<Account> = Account.entries.filter { isLoggedIn(it) }.toSet()

    /** Writes a fresh cookies.txt for yt-dlp, or returns null when not logged in anywhere. */
    suspend fun cookieFile(ctx: Context): File? {
        val expires = System.currentTimeMillis() / 1000 + ONE_YEAR
        val blocks = Account.entries.mapNotNull { acc ->
            val header = cookieHeader(acc)
            if (CookieFile.hasCookie(header, acc.sessionCookie)) CookieFile.lines(acc.cookieDomain, header, expires) else null
        }
        val file = File(ctx.filesDir, "cookies.txt")
        if (blocks.isEmpty()) {
            file.delete()
            return null
        }
        withContext(Dispatchers.IO) { file.writeText(CookieFile.build(blocks)) }
        return file
    }

    /** Clears that site's cookies (falls back to clearing all WebView cookies if they don't go away). */
    suspend fun logout(account: Account) = withContext(Dispatchers.Main) {
        val cm = CookieManager.getInstance()
        val bare = account.cookieDomain.removePrefix(".")
        val urls = listOf(account.siteUrl, "https://$bare", "https://m.$bare", "https://i.$bare")
        for ((name, _) in CookieFile.parseHeader(cm.getCookie(account.siteUrl))) {
            for (url in urls) {
                cm.setCookie(url, "$name=; Max-Age=0; Path=/")
                cm.setCookie(url, "$name=; Max-Age=0; Path=/; Domain=${account.cookieDomain}")
            }
        }
        cm.flush()
        if (CookieFile.hasCookie(cm.getCookie(account.siteUrl), account.sessionCookie)) {
            cm.removeAllCookies(null)
            cm.flush()
        }
    }
}
