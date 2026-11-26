package com.socialsaver.core

/** Turns yt-dlp's raw format list into the short list of choices the user sees. */
object QualityPlanner {
    const val MAX_VIDEO_OPTIONS = 6

    fun plan(formats: List<RawFormat>): List<QualityOption> {
        if (formats.isEmpty()) return emptyList()

        val videos = formats.filter { it.vcodec != "none" && (it.height ?: 0) > 0 }
        val audioOnly = formats.filter { it.vcodec == "none" && it.acodec != "none" }
        val bestAudio = audioOnly.mapNotNull { it.filesize }.maxOrNull()

        val options = mutableListOf<QualityOption>()
        val seenLabels = mutableSetOf<String>()

        for (h in videos.mapNotNull { it.height }.distinct().sortedDescending()) {
            val atHeight = videos.filter { it.height == h }
            val width = atHeight.mapNotNull { it.width }.firstOrNull { it > 0 }
            // Vertical videos (Reels) are 1080x1920: call that "1080p", not "1920p".
            val label = "${if (width != null) minOf(width, h) else h}p"
            if (!seenLabels.add(label)) continue

            val rawSize = atHeight.mapNotNull { it.filesize }.maxOrNull()
            val videoOnly = atHeight.all { it.acodec == "none" }
            val size = rawSize?.let { if (videoOnly) it + (bestAudio ?: 0) else it }

            options += QualityOption(label, "bv*[height<=$h]+ba/b[height<=$h]/bv*+ba/b", OptionKind.VIDEO, size)
            if (options.size == MAX_VIDEO_OPTIONS) break
        }

        if (options.isEmpty() && formats.any { it.vcodec != "none" }) {
            options += QualityOption("Best quality", "bv*+ba/b", OptionKind.VIDEO)
        }
        if (!formats.all { it.acodec == "none" }) {
            options += QualityOption("Audio only (MP3)", "ba/b", OptionKind.AUDIO, bestAudio)
        }
        return options
    }
}
