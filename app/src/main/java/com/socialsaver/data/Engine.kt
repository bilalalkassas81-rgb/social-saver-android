package com.socialsaver.data

import android.content.Context
import com.socialsaver.core.ErrorText
import com.socialsaver.core.InfoParser
import com.socialsaver.core.MediaInfo
import com.socialsaver.core.OptionKind
import com.socialsaver.core.Platform
import com.socialsaver.core.QualityOption
import com.socialsaver.core.UserFacingException
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** yt-dlp (bundled through youtubedl-android) plus fallbacks for when it can't get a post. */
object Engine {
    private const val PROCESS_ID = "social-saver-download"
    private const val MAX_ITEMS = "1:20"
    private val initLock = Mutex()
    @Volatile private var ready = false

    suspend fun ensureReady(ctx: Context) = withContext(Dispatchers.IO) {
        initLock.withLock {
            if (!ready) {
                try {
                    YoutubeDL.getInstance().init(ctx.applicationContext)
                    FFmpeg.getInstance().init(ctx.applicationContext)
                    ready = true
                } catch (e: Exception) {
                    throw UserFacingException("Couldn't start the download engine: ${e.message ?: "unknown error"}")
                }
            }
        }
    }

    private fun YoutubeDLRequest.withCookies(file: File?) = apply {
        if (file != null) addOption("--cookies", file.absolutePath)
    }

    suspend fun fetch(ctx: Context, url: String, platform: Platform): MediaInfo {
        ensureReady(ctx)
        val account = Account.forPlatform(platform)
        val cookieHeader = account?.let { Accounts.cookieHeader(it) }
        val loggedIn = account != null && Accounts.isLoggedIn(account)
        val cookies = Accounts.cookieFile(ctx)

        return withContext(Dispatchers.IO) {
            // 1. yt-dlp: real quality choices.
            val failure: Exception? = try {
                val request = YoutubeDLRequest(url)
                    .addOption("-J")
                    .addOption("--playlist-items", MAX_ITEMS)
                    .withCookies(cookies)
                val info = InfoParser.parse(YoutubeDL.getInstance().execute(request).out, url, platform)
                if (info.options.isNotEmpty()) return@withContext info
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }

            // 2. Fallbacks.
            val fallback = runCatching {
                when (platform) {
                    Platform.INSTAGRAM ->
                        if (loggedIn && cookieHeader != null) Fallbacks.instagram(url, cookieHeader)
                        else Fallbacks.openGraphPhoto(url, platform)
                    Platform.X -> Fallbacks.twitter(url)
                    Platform.FACEBOOK -> Fallbacks.openGraphPhoto(url, platform)
                    Platform.OTHER -> null
                }
            }.getOrNull()
            if (fallback != null) return@withContext fallback

            // 3. Explain what to do.
            if (account != null && !loggedIn) {
                throw UserFacingException(
                    "${platform.label} wants you to be logged in to download this. Log in once and try again.",
                    loginFor = platform,
                )
            }
            throw when {
                failure is UserFacingException -> failure
                failure != null -> UserFacingException(ErrorText.friendly(failure.message))
                else -> UserFacingException("No video or photo was found in this post.")
            }
        }
    }

    /** Downloads into a temp folder, then moves the results to Downloads/SocialSaver. */
    suspend fun download(
        ctx: Context,
        info: MediaInfo,
        option: QualityOption,
        onProgress: (Float?) -> Unit,
    ): List<SavedFile> {
        ensureReady(ctx)
        val cookies = if (option.kind == OptionKind.DIRECT) null else Accounts.cookieFile(ctx)
        return withContext(Dispatchers.IO) {
            val stamp = System.currentTimeMillis()
            val dir = File(ctx.cacheDir, "downloads/$stamp").apply { mkdirs() }
            try {
                if (option.kind == OptionKind.DIRECT) {
                    info.directItems.forEachIndexed { i, item ->
                        ensureActive()
                        onProgress(i.toFloat() / info.directItems.size)
                        Fallbacks.downloadItem(item, dir, "${info.platform.label.lowercase()}_${stamp}_${i + 1}")
                    }
                    onProgress(1f)
                } else {
                    onProgress(null)
                    val request = YoutubeDLRequest(info.url)
                        .addOption("-o", "${dir.absolutePath}/%(extractor_key)s_%(id)s.%(ext)s")
                        .addOption("--playlist-items", MAX_ITEMS)
                        .addOption("-f", option.selector)
                        .addOption("--no-mtime")
                        .withCookies(cookies)
                    if (option.kind == OptionKind.VIDEO) {
                        request.addOption("--merge-output-format", "mp4")
                    } else {
                        request.addOption("-x").addOption("--audio-format", "mp3")
                    }
                    try {
                        YoutubeDL.getInstance().execute(request, PROCESS_ID) { progress, _, _ ->
                            if (progress >= 0f) onProgress((progress / 100f).coerceIn(0f, 1f))
                        }
                    } catch (e: YoutubeDL.CanceledException) {
                        throw CancellationException("Download cancelled")
                    } catch (e: YoutubeDLException) {
                        throw UserFacingException(ErrorText.friendly(e.message))
                    }
                }

                val files = dir.listFiles().orEmpty()
                    .filter { f ->
                        f.isFile && f.length() > 0 &&
                            listOf(".part", ".ytdl", ".tmp").none { f.name.endsWith(it) } &&
                            !f.name.contains(".temp.")
                    }
                    .sortedBy { it.name }
                if (files.isEmpty()) throw UserFacingException("The download finished but no file was produced.")
                files.map { MediaSaver.save(ctx, it) }
            } finally {
                dir.deleteRecursively()
            }
        }
    }

    fun cancel() {
        try {
            YoutubeDL.getInstance().destroyProcessById(PROCESS_ID)
        } catch (_: Exception) {
        }
    }

    suspend fun version(ctx: Context): String? = try {
        ensureReady(ctx)
        withContext(Dispatchers.IO) { YoutubeDL.getInstance().version(ctx.applicationContext) }
    } catch (e: Exception) {
        null
    }

    suspend fun update(ctx: Context): String {
        ensureReady(ctx)
        return withContext(Dispatchers.IO) {
            try {
                when (YoutubeDL.getInstance().updateYoutubeDL(ctx.applicationContext, YoutubeDL.UpdateChannel.STABLE)) {
                    YoutubeDL.UpdateStatus.DONE ->
                        "Updated to ${YoutubeDL.getInstance().version(ctx.applicationContext) ?: "the latest version"}."
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Already up to date."
                    null -> "Update finished."
                }
            } catch (e: Exception) {
                throw UserFacingException("Update failed. Check your internet connection and try again.")
            }
        }
    }
}
