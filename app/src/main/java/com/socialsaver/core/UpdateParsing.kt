package com.socialsaver.core

import org.json.JSONObject

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long?,
)

/** Reads GitHub's "latest release" answer and decides whether it's newer than this install. */
object UpdateParsing {
    private val tagRegex = Regex("""^build-(\d+)$""")

    fun parseLatest(json: String, currentVersionCode: Int, abis: List<String>): UpdateInfo? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val code = o.str("tag_name")?.let { tagRegex.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (code <= currentVersionCode) return null

        val arr = o.optJSONArray("assets")
        val apks = (0 until (arr?.length() ?: 0)).mapNotNull { arr!!.optJSONObject(it) }
            .filter { it.str("name")?.endsWith(".apk") == true && it.str("browser_download_url") != null }
        val wanted = if (abis.none { it.startsWith("arm64") } && abis.any { it.startsWith("armeabi") }) "armv7" else "arm64"
        val apk = apks.firstOrNull { it.str("name")!!.contains(wanted) } ?: apks.firstOrNull() ?: return null

        return UpdateInfo(
            versionCode = code,
            versionName = "1.0.$code",
            apkUrl = apk.str("browser_download_url")!!,
            pageUrl = o.str("html_url").orEmpty(),
            sizeBytes = apk.num("size")?.toLong(),
        )
    }
}
