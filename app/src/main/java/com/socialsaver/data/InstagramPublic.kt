package com.socialsaver.data

import com.socialsaver.core.InstagramParsing
import com.socialsaver.core.InstagramParsing.IgPost
import org.json.JSONObject

/**
 * Logged-out Instagram, done the way instagram.com does it in a browser:
 * open the home page for a session token, then ask the GraphQL endpoint for the post.
 * yt-dlp only does this with browser impersonation, which Android's Python lacks.
 */
object InstagramPublic {
    private fun apiHeaders(referer: String) = mapOf(
        "X-IG-App-ID" to InstagramParsing.APP_ID,
        "X-ASBD-ID" to InstagramParsing.ASBD_ID,
        "X-IG-WWW-Claim" to "0",
        "Origin" to "https://www.instagram.com",
        "Referer" to referer,
        "Accept" to "*/*",
    )

    /** Blocking. Adds a line to [log] explaining any failure. */
    fun fetch(postUrl: String, log: MutableList<String>): IgPost? {
        val code = InstagramParsing.shortcode(postUrl) ?: return null
        val mediaId = InstagramParsing.mediaId(code)
        val session = Http.Session()

        val home = Http.request(
            "https://www.instagram.com/",
            Http.DESKTOP_UA,
            mapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"),
            session = session,
        )
        val lsd = InstagramParsing.extractLsd(home.body)
        if (lsd == null) {
            log += "direct: home page HTTP ${home.code}, no session token"
            return null
        }

        val ruling = runCatching {
            Http.request(
                "https://www.instagram.com/api/v1/web/get_ruling_for_content/?content_type=MEDIA&target_id=$mediaId",
                Http.DESKTOP_UA, apiHeaders(postUrl), session = session,
            )
        }.getOrNull()
        val rulingOk = runCatching { JSONObject(ruling?.body ?: "").optString("status") == "ok" }.getOrDefault(false)
        val csrf = if (rulingOk) session.cookies["csrftoken"].orEmpty() else ""

        val r = Http.request(
            "https://www.instagram.com/api/graphql",
            Http.DESKTOP_UA,
            apiHeaders(postUrl) + mapOf(
                "X-FB-Friendly-Name" to InstagramParsing.GRAPHQL_NAME,
                "X-FB-LSD" to lsd,
                "X-CSRFToken" to csrf,
                "X-Requested-With" to "XMLHttpRequest",
            ),
            form = InstagramParsing.graphqlForm(lsd, mediaId),
            session = session,
        )
        val post = runCatching { InstagramParsing.parseGraphql(r.body) }.getOrNull()
        if (post == null) log += "direct: graphql HTTP ${r.code}: ${r.body.take(150).replace('\n', ' ')}"
        return post
    }
}
