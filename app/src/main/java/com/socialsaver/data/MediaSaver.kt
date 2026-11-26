package com.socialsaver.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

data class SavedFile(val uri: String, val mime: String, val name: String)

/** Copies a finished download into the public Downloads/SocialSaver folder. */
object MediaSaver {
    const val FOLDER = "SocialSaver"

    suspend fun save(ctx: Context, file: File): SavedFile {
        val mime = mimeOf(file.name)
        return if (Build.VERSION.SDK_INT >= 29) saveScoped(ctx, file, mime) else saveLegacy(ctx, file, mime)
    }

    fun mimeOf(name: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

    @RequiresApi(29)
    private fun saveScoped(ctx: Context, file: File, mime: String): SavedFile {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Couldn't create the file in Downloads.")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Couldn't write the file.")
            out.use { o -> file.inputStream().use { it.copyTo(o) } }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return SavedFile(uri.toString(), mime, file.name)
    }

    @Suppress("DEPRECATION")
    private suspend fun saveLegacy(ctx: Context, file: File, mime: String): SavedFile {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        dir.mkdirs()
        var target = File(dir, file.name)
        var n = 1
        while (target.exists()) {
            target = File(dir, "${file.nameWithoutExtension} ($n).${file.extension}")
            n++
        }
        file.copyTo(target)
        val uri = suspendCancellableCoroutine<Uri?> { cont ->
            MediaScannerConnection.scanFile(ctx, arrayOf(target.absolutePath), arrayOf(mime)) { _, u ->
                if (cont.isActive) cont.resume(u)
            }
        }
        return SavedFile((uri ?: Uri.fromFile(target)).toString(), mime, target.name)
    }
}
