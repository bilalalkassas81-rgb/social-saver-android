package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstagramParsingTest {
    @Test fun shortcodes() {
        assertEquals("C1abc-_9", InstagramParsing.shortcode("https://www.instagram.com/reel/C1abc-_9/?igsh=x"))
        assertEquals("ABC", InstagramParsing.shortcode("https://instagram.com/p/ABC/"))
        assertEquals("XYZ", InstagramParsing.shortcode("https://www.instagram.com/someone/p/XYZ/"))
        assertNull(InstagramParsing.shortcode("https://www.instagram.com/someone/"))
    }

    @Test fun shortcodeToMediaIdMatchesInstagram() {
        // Expected values verified against yt-dlp's own _id_to_pk().
        assertEquals("1", InstagramParsing.mediaId("B"))
        assertEquals("64", InstagramParsing.mediaId("BA"))
        assertEquals("2014355111653905518", InstagramParsing.mediaId("Bv0bU0xn5Ru"))
    }

    @Test fun singleVideoPicksLargest() {
        val json = """{"items":[{"media_type":2,"caption":{"text":"My reel"},"user":{"username":"ali"},
            "video_versions":[{"url":"https://v/small.mp4","width":480,"height":854},{"url":"https://v/big.mp4","width":1080,"height":1920}],
            "image_versions2":{"candidates":[{"url":"https://i/small.jpg","width":320,"height":568},{"url":"https://i/big.jpg","width":1080,"height":1920}]}}]}"""
        val post = InstagramParsing.parseMediaInfo(json)!!
        assertEquals("My reel", post.title)
        assertEquals("https://i/big.jpg", post.thumbnail)
        assertEquals(listOf(DirectItem("https://v/big.mp4", true)), post.items)
    }

    @Test fun carouselMixesPhotosAndVideos() {
        val json = """{"items":[{"media_type":8,"user":{"username":"sara"},"carousel_media":[
            {"image_versions2":{"candidates":[{"url":"https://i/1.jpg","width":1080,"height":1350}]}},
            {"video_versions":[{"url":"https://v/2.mp4","width":720,"height":1280}],"image_versions2":{"candidates":[{"url":"https://i/2.jpg","width":720,"height":1280}]}}
        ]}]}"""
        val post = InstagramParsing.parseMediaInfo(json)!!
        assertEquals("Post by sara", post.title)
        assertEquals(listOf(DirectItem("https://i/1.jpg", false), DirectItem("https://v/2.mp4", true)), post.items)
    }

    @Test fun emptyResponse() {
        assertNull(InstagramParsing.parseMediaInfo("""{"items":[]}"""))
    }
}
