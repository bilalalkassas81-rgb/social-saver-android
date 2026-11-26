package com.socialsaver.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openMedia(ctx: Context, uri: String, mime: String) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(uri), mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Couldn't open this file. It may have been deleted.", Toast.LENGTH_SHORT).show()
    }
}

fun shareMedia(ctx: Context, uris: List<String>, mimes: List<String>) {
    if (uris.isEmpty()) return
    val type = mimes.distinct().singleOrNull() ?: "*/*"
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, Uri.parse(uris.first()))
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).setType(type)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris.map(Uri::parse)))
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        ctx.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Couldn't share this file.", Toast.LENGTH_SHORT).show()
    }
}
