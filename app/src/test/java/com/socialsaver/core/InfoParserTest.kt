package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoParserTest {
    private val single = """
        {"id":"C1abc","title":"Video by someone","thumbnail":"https://cdn/t.jpg","extractor_key":"Instagram",
         "formats":[
           {"format_id":"dash-a","vcodec":"none","acodec":"mp4a.40.2","filesize":900000},
           {"format_id":"dash-720","vcodec":"avc1","acodec":"none","width":720,"height":1280,"filesize":4000000},
           {"format_id":"dash-1080","vcodec":"avc1","acodec":"none","width":1080,"height":1920,"filesize_approx":8000000.0},
           {"format_id":"8","vcodec":null,"acodec":null,"width":null,"height":null}
         ]}
    """.trimIndent()

    @Test fun parsesSingleVideo() {
        val info = InfoParser.parse(single, "u", Platform.INSTAGRAM)
        assertEquals("Video by someone", info.title)
        assertEquals("https://cdn/t.jpg", info.thumbnail)
        assertEquals(listOf("1080p", "720p", "Audio only (MP3)"), info.options.map { it.label })
        assertEquals(8_900_000L, info.options[0].sizeBytes)
        assertEquals(1, info.itemCount)
    }

    @Test fun ignoresLeadingNoise() {
        val info = InfoParser.parse("WARNING: something\n$single", "u", Platform.INSTAGRAM)
        assertEquals(3, info.options.size)
    }

    @Test fun parsesCarouselPlaylist() {
        val json = """
            {"_type":"playlist","title":"Post by someone","entries":[
              {"id":"1","thumbnail":"https://cdn/1.jpg","formats":[{"format_id":"a","vcodec":"h264","acodec":"aac","width":640,"height":640}]},
              {"id":"2","formats":[{"format_id":"b","vcodec":"h264","acodec":"aac","width":640,"height":640}]}
            ]}
        """.trimIndent()
        val info = InfoParser.parse(json, "u", Platform.INSTAGRAM)
        assertEquals(2, info.itemCount)
        assertEquals("Post by someone", info.title)
        assertEquals("https://cdn/1.jpg", info.thumbnail)
        assertEquals("640p", info.options[0].label)
    }

    @Test fun directUrlWithoutFormats() {
        val json = """{"id":"9","title":"  Tweet   text  ","url":"https://v/9.mp4","width":1280,"height":720,"vcodec":"avc1","acodec":"aac",
            "thumbnails":[{"url":"https://t/small.jpg"},{"url":"https://t/big.jpg"}]}"""
        val info = InfoParser.parse(json, "u", Platform.X)
        assertEquals("Tweet text", info.title)
        assertEquals("https://t/big.jpg", info.thumbnail)
        assertEquals("720p", info.options[0].label)
    }

    @Test fun noFormatsMeansNoOptions() {
        val info = InfoParser.parse("""{"id":"1","title":""}""", "u", Platform.FACEBOOK)
        assertTrue(info.options.isEmpty())
        assertEquals("Facebook post", info.title)
    }

    @Test(expected = UserFacingException::class)
    fun garbageThrowsFriendlyError() {
        InfoParser.parse("not json", "u", Platform.X)
    }
}
