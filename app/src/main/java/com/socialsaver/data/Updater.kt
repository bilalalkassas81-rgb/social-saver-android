package com.socialsaver.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.socialsaver.BuildConfig
import com.socialsaver.core.UpdateInfo
import com.socialsaver.core.UpdateParsing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Checks GitHub Releases for a newer build, downloads it and hands it to Android's installer. */
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

    private fun dir(ctx: Context) = File(ctx.cacheDir, "updates")

    /** Removes an already-installed update file. */
    fun cleanup(ctx: Context) {
        dir(ctx).deleteRecursively()
    }

    suspend fun download(ctx: Context, info: UpdateInfo, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val d = dir(ctx).apply { deleteRecursively(); mkdirs() }
        val file = File(d, "SocialSaver-${info.versionCode}.apk")
        Http.download(info.apkUrl, file, onProgress = onProgress)
        file
    }

    /**
     * Opens Android's installer. Returns false when the user first has to allow
     * "Install unknown apps" for Social Saver (we open that settings page for them).
     */
    fun install(ctx: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    fun openPage(ctx: Context, url: String) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
