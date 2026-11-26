package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityPlannerTest {
    private fun v(h: Int, w: Int?, size: Long? = null, audio: Boolean = false) =
        RawFormat("v$h", w, h, "avc1", if (audio) "mp4a" else "none", size)
    private val audio = RawFormat("a", null, null, "none", "mp4a", 1_000_000)

    @Test fun reelUsesShortSideForLabelsAndSortsHighFirst() {
        val opts = QualityPlanner.plan(listOf(v(1280, 720), v(1920, 1080), v(640, 360), audio))
        assertEquals(listOf("1080p", "720p", "360p", "Audio only (MP3)"), opts.map { it.label })
        assertEquals("bv*[height<=1920]+ba/b[height<=1920]/bv*+ba/b", opts[0].selector)
        assertEquals(OptionKind.AUDIO, opts.last().kind)
    }

    @Test fun videoOnlySizeIncludesAudio() {
        val opts = QualityPlanner.plan(listOf(v(720, 1280, size = 5_000_000), audio))
        assertEquals(6_000_000L, opts[0].sizeBytes)
    }

    @Test fun muxedSizeIsNotDoubleCounted() {
        val opts = QualityPlanner.plan(listOf(v(720, 1280, size = 5_000_000, audio = true), audio))
        assertEquals(5_000_000L, opts[0].sizeBytes)
    }

    @Test fun duplicateHeightsCollapse() {
        val opts = QualityPlanner.plan(listOf(v(720, 1280), v(720, 1280), audio))
        assertEquals(1, opts.count { it.kind == OptionKind.VIDEO })
    }

    @Test fun capsNumberOfVideoOptions() {
        val many = (1..10).map { v(it * 100, null) }
        assertEquals(QualityPlanner.MAX_VIDEO_OPTIONS, QualityPlanner.plan(many).count { it.kind == OptionKind.VIDEO })
    }

    @Test fun unknownHeightsFallBackToBest() {
        val opts = QualityPlanner.plan(listOf(RawFormat("x", null, null, null, null, null)))
        assertEquals("Best quality", opts[0].label)
        assertTrue(opts.any { it.kind == OptionKind.AUDIO })
    }

    @Test fun silentVideoHasNoAudioOption() {
        val opts = QualityPlanner.plan(listOf(v(720, 1280)))
        assertTrue(opts.none { it.kind == OptionKind.AUDIO })
    }

    @Test fun emptyGivesNothing() {
        assertTrue(QualityPlanner.plan(emptyList()).isEmpty())
    }
}
