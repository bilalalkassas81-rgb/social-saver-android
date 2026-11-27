package com.socialsaver.data

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.socialsaver.core.InstagramParsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Backup for [InstagramPublic]: runs the same request inside an invisible copy of the phone's
 * Chrome (WebView), so Instagram sees a real browser in every detail.
 */
object InstagramWeb {

    private class Bridge(private val onResult: (String?, String) -> Unit) {
        @JavascriptInterface fun done(body: String) = onResult(body, "ok")
        @JavascriptInterface fun fail(message: String) = onResult(null, message)
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun graphql(ctx: Context, mediaId: String, log: MutableList<String>): String? {
        val result = withTimeoutOrNull(30_000) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<String?> { cont ->
                    val finished = AtomicBoolean(false)
                    val wv = WebView(ctx.applicationContext)
                    fun finish(body: String?, note: String) {
                        if (!finished.compareAndSet(false, true)) return
                        if (body == null) log += "browser: $note"
                        wv.post { wv.stopLoading(); wv.destroy() }
                        if (cont.isActive) cont.resume(body)
                    }
                    wv.settings.javaScriptEnabled = true
                    wv.settings.domStorageEnabled = true
                    wv.settings.userAgentString = wv.settings.userAgentString.replace("; wv", "")
                    CookieManager.getInstance().setAcceptCookie(true)
                    wv.addJavascriptInterface(Bridge(::finish), "SocialSaver")
                    var injected = false
                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (!injected) {
                                injected = true
                                view.evaluateJavascript(script(mediaId), null)
                            }
                        }
                    }
                    wv.loadUrl("https://www.instagram.com/")
                    cont.invokeOnCancellation { finish(null, "timed out") }
                }
            }
        }
        return result
    }

    private fun script(mediaId: String): String {
        val form = InstagramParsing.graphqlForm("LSD_PLACEHOLDER", mediaId)
            .entries.joinToString(",") { (k, v) -> "[" + jsString(k) + "," + jsString(v) + "]" }
        return """
            (function () {
              function fail(m) { SocialSaver.fail(String(m)); }
              try {
                var html = document.documentElement.innerHTML;
                var m = html.match(/"LSD",\[\],\{"token":"([^"]+)"/) || html.match(/"lsd":"([^"]+)"/) || html.match(/name="lsd" value="([^"]+)"/);
                if (!m) { fail('no session token on ' + location.pathname); return; }
                var lsd = m[1];
                var c = document.cookie.match(/(?:^|; )csrftoken=([^;]+)/);
                var body = new URLSearchParams();
                [$form].forEach(function (p) { body.append(p[0], p[1] === 'LSD_PLACEHOLDER' ? lsd : p[1]); });
                fetch('/api/graphql', {
                  method: 'POST',
                  credentials: 'include',
                  headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-IG-App-ID': '${InstagramParsing.APP_ID}',
                    'X-ASBD-ID': '${InstagramParsing.ASBD_ID}',
                    'X-IG-WWW-Claim': '0',
                    'X-FB-LSD': lsd,
                    'X-FB-Friendly-Name': '${InstagramParsing.GRAPHQL_NAME}',
                    'X-CSRFToken': c ? c[1] : '',
                    'X-Requested-With': 'XMLHttpRequest'
                  },
                  body: body.toString()
                }).then(function (r) {
                  return r.text().then(function (t) {
                    if (r.ok) SocialSaver.done(t); else fail('graphql HTTP ' + r.status + ': ' + t.slice(0, 150));
                  });
                }).catch(function (e) { fail('network error ' + e); });
              } catch (e) { fail('script error ' + e); }
            })();
        """.trimIndent()
    }

    private fun jsString(s: String): String =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"
}
