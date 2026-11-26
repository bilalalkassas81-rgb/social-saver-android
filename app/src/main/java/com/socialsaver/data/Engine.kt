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

/** Wraps yt-dlp (bundled through youtubedl-android) plus the photo fallback. */
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

    suspend fun fetch(ctx: Context, url: String, platform: Platform): MediaInfo = withContext(Dispatchers.IO) {
        ensureReady(ctx)
        val failure: Exception? = try {
            val request = YoutubeDLRequest(url)
                .addOption("-J")
                .addOption("--playlist-items", MAX_ITEMS)
            val info = InfoParser.parse(YoutubeDL.getInstance().execute(request).out, url, platform)
            if (info.options.isNotEmpty()) return@withContext info
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e
        }
        // No video found (or yt-dlp failed): maybe it's a photo post.
        PhotoFallback.find(url, platform)?.let { return@withContext it }
        throw UserFacingException(
            if (failure == null) "No video or photo was found in this post."
            else (failure as? UserFacingException)?.message ?: ErrorText.friendly(failure.message)
        )
    }

    /** Downloads into a temp folder, then moves the results to Downloads/SocialSaver. */
    suspend fun download(
        ctx: Context,
        info: MediaInfo,
        option: QualityOption,
        onProgress: (Float?) -> Unit,
    ): List<SavedFile> = withContext(Dispatchers.IO) {
        ensureReady(ctx)
        val stamp = System.currentTimeMillis()
        val dir = File(ctx.cacheDir, "downloads/$stamp").apply { mkdirs() }
        try {
            if (option.kind == OptionKind.PHOTOS) {
                info.photoUrls.forEachIndexed { i, photo ->
                    ensureActive()
                    val base = "${info.platform.label.lowercase()}_${stamp}_${i + 1}"
                    PhotoFallback.downloadPhoto(photo, dir, base)
                    onProgress((i + 1).toFloat() / info.photoUrls.size)
                }
            } else {
                onProgress(null)
                val request = YoutubeDLRequest(info.url)
                    .addOption("-o", "${dir.absolutePath}/%(extractor_key)s_%(id)s.%(ext)s")
                    .addOption("--playlist-items", MAX_ITEMS)
                    .addOption("-f", option.selector)
                    .addOption("--no-mtime")
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

    fun cancel() {
        try {
            YoutubeDL.getInstance().destroyProcessById(PROCESS_ID)
        } catch (_: Exception) {
        }
    }

    suspend fun version(ctx: Context): String? = withContext(Dispatchers.IO) {
        try {
            ensureReady(ctx)
            YoutubeDL.getInstance().version(ctx.applicationContext)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun update(ctx: Context): String = withContext(Dispatchers.IO) {
        ensureReady(ctx)
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
