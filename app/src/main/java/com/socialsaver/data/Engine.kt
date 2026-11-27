package com.socialsaver.data

import android.content.Context
import com.socialsaver.core.CookieFile
import com.socialsaver.core.ErrorText
import com.socialsaver.core.InfoParser
import com.socialsaver.core.InstagramParsing
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

/** yt-dlp (bundled through youtubedl-android) plus our own fetchers for when it can't get a post. */
object Engine {
    private const val PROCESS_ID = "social-saver-download"
    private const val MAX_ITEMS = "1:20"
    private const val UPDATE_EVERY_MS = 24L * 3600 * 1000
    private val initLock = Mutex()
    /** yt-dlp must not run while it's being updated. */
    private val ytdlpLock = Mutex()
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
        val log = mutableListOf<String>()
        val account = Account.forPlatform(platform)
        val cookieHeader = account?.let { Accounts.cookieHeader(it) }
        val loggedIn = account != null && CookieFile.hasCookie(cookieHeader, account.sessionCookie)

        // Instagram: our own browser-style requests first. yt-dlp can't do logged-out Instagram on Android.
        if (platform == Platform.INSTAGRAM) {
            instagram(ctx, url, if (loggedIn) cookieHeader else null, log)?.let { return it }
        }

        ensureReady(ctx)
        val cookies = Accounts.cookieFile(ctx)
        var ytdlpError: String? = null
        val viaYtdlp = withContext(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(url)
                    .addOption("-J")
                    .addOption("--playlist-items", MAX_ITEMS)
                    .withCookies(cookies)
                val out = ytdlpLock.withLock { YoutubeDL.getInstance().execute(request).out }
                InfoParser.parse(out, url, platform).takeIf { it.options.isNotEmpty() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ytdlpError = (e as? UserFacingException)?.message ?: ErrorText.friendly(e.message)
                log += "yt-dlp: ${e.message?.lines()?.lastOrNull { it.isNotBlank() }?.take(200)}"
                null
            }
        }
        if (viaYtdlp != null) return viaYtdlp

        val fallback = withContext(Dispatchers.IO) {
            runCatching {
                when (platform) {
                    Platform.X -> Fallbacks.twitter(url)
                    Platform.INSTAGRAM, Platform.FACEBOOK -> Fallbacks.openGraphPhoto(url, platform)
                    Platform.OTHER -> null
                }
            }.onFailure { log += "fallback: ${it.message}" }.getOrNull()
        }
        if (fallback != null) return fallback

        val details = log.joinToString("\n").ifBlank { null }
        if (account != null && !loggedIn) {
            throw UserFacingException(
                "${platform.label} didn't share this post with the app. It may be private, or ${platform.label} " +
                    "is limiting requests from your connection for a while. Logging in usually gets around it.",
                loginFor = platform,
                details = details,
            )
        }
        throw UserFacingException(ytdlpError ?: "No video or photo was found in this post.", details = details)
    }

    /** Logged in: Instagram's API. Logged out: GraphQL directly, then through an invisible browser. */
    private suspend fun instagram(ctx: Context, url: String, cookieHeader: String?, log: MutableList<String>): MediaInfo? {
        val code = InstagramParsing.shortcode(url) ?: return null // profiles, stories: leave to yt-dlp

        if (cookieHeader != null) {
            withContext(Dispatchers.IO) {
                runCatching { Fallbacks.instagram(url, cookieHeader) }
                    .onFailure { log += "account: ${it.message}" }.getOrNull()
            }?.let { return it }
        }

        withContext(Dispatchers.IO) {
            runCatching { InstagramPublic.fetch(url, log) }
                .onFailure { log += "direct: ${it.javaClass.simpleName} ${it.message}" }.getOrNull()
        }?.let { return Fallbacks.fromIgPost(url, it) }

        val json = InstagramWeb.graphql(ctx, InstagramParsing.mediaId(code), log) ?: return null
        val post = runCatching { InstagramParsing.parseGraphql(json) }.getOrNull()
        if (post == null) log += "browser: answer had no media: ${json.take(150).replace('\n', ' ')}"
        return post?.let { Fallbacks.fromIgPost(url, it) }
    }

    /** Downloads into a temp folder, then moves the results to Downloads/SocialSaver. */
    suspend fun download(
        ctx: Context,
        info: MediaInfo,
        option: QualityOption,
        onProgress: (Float?) -> Unit,
    ): List<SavedFile> {
        if (option.kind != OptionKind.DIRECT) ensureReady(ctx)
        val cookies = if (option.kind == OptionKind.DIRECT) null else Accounts.cookieFile(ctx)
        return withContext(Dispatchers.IO) {
            val stamp = System.currentTimeMillis()
            val dir = File(ctx.cacheDir, "downloads/$stamp").apply { mkdirs() }
            try {
                if (option.kind == OptionKind.DIRECT) {
                    val items = option.items.ifEmpty { info.directItems }
                    items.forEachIndexed { i, item ->
                        ensureActive()
                        onProgress(i.toFloat() / items.size)
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
                        ytdlpLock.withLock {
                            YoutubeDL.getInstance().execute(request, PROCESS_ID) { progress, _, _ ->
                                if (progress >= 0f) onProgress((progress / 100f).coerceIn(0f, 1f))
                            }
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
        val message = withContext(Dispatchers.IO) {
            ytdlpLock.withLock {
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
        prefs(ctx).edit().putLong("last_update", System.currentTimeMillis()).apply()
        return message
    }

    /** Keeps yt-dlp fresh on its own: at most once a day, silently. */
    suspend fun autoUpdateIfDue(ctx: Context) {
        val last = prefs(ctx).getLong("last_update", 0L)
        if (System.currentTimeMillis() - last < UPDATE_EVERY_MS) return
        runCatching { update(ctx) }
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("engine", Context.MODE_PRIVATE)
}
