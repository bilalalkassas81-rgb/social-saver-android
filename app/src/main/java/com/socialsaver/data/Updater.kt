package com.socialsaver.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.socialsaver.BuildConfig
import com.socialsaver.core.UpdateInfo
import com.socialsaver.core.UpdateParsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Checks GitHub Releases for a newer build. "Update now" opens the APK link in the browser,
 * which downloads and installs it, so the app itself never needs permission to install apps.
 */
object Updater {
    private const val LATEST_URL =
        "https://api.github.com/repos/bilalalkassas81-rgb/social-saver-android/releases/latest"

    sealed class Check {
        data class Available(val info: UpdateInfo) : Check()
        data object UpToDate : Check()
        data class Failed(val reason: String) : Check()
    }

    suspend fun check(): Check = withContext(Dispatchers.IO) {
        try {
            val r = Http.request(LATEST_URL, Http.BROWSER_UA, mapOf("Accept" to "application/vnd.github+json"))
            if (r.code != 200) return@withContext Check.Failed("HTTP ${r.code}")
            val info = UpdateParsing.parseLatest(r.body, BuildConfig.VERSION_CODE, Build.SUPPORTED_ABIS.toList())
            if (info != null) Check.Available(info) else Check.UpToDate
        } catch (e: Exception) {
            Check.Failed(e.message ?: "network error")
        }
    }

    fun openPage(ctx: Context, url: String) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
