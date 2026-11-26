package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoParsingTest {
    @Test fun tweetIds() {
        assertEquals("1790000000000000001", PhotoParsing.tweetId("https://x.com/nasa/status/1790000000000000001?s=20"))
        assertEquals("42", PhotoParsing.tweetId("https://twitter.com/i/web/status/42"))
        assertNull(PhotoParsing.tweetId("https://x.com/nasa"))
    }

    @Test fun twitterOriginalQuality() {
        assertEquals("https://pbs.twimg.com/media/A.jpg?name=orig", PhotoParsing.twitterOriginal("https://pbs.twimg.com/media/A.jpg"))
        assertEquals("https://pbs.twimg.com/media/A?format=jpg&name=orig", PhotoParsing.twitterOriginal("https://pbs.twimg.com/media/A?format=jpg"))
        assertEquals("https://pbs.twimg.com/media/A?name=large", PhotoParsing.twitterOriginal("https://pbs.twimg.com/media/A?name=large"))
    }

    @Test fun parsesFxTwitter() {
        val json = """{"code":200,"tweet":{"text":"Two pics","author":{"name":"NASA"},
            "media":{"photos":[{"url":"https://pbs.twimg.com/media/A.jpg"},{"url":"https://pbs.twimg.com/media/B.png"}]}}}"""
        val r = PhotoParsing.parseFxTwitter(json)
        assertEquals("NASA - Two pics", r.title)
        assertEquals(2, r.items.size)
        assertEquals(DirectItem("https://pbs.twimg.com/media/B.png?name=orig", false), r.items[1])
        assertEquals("https://pbs.twimg.com/media/A.jpg", r.thumbnail)
    }

    @Test fun fxTwitterVideo() {
        val json = """{"tweet":{"media":{"videos":[{"url":"https://video.twimg.com/v.mp4","thumbnail_url":"https://pbs.twimg.com/t.jpg"}]}}}"""
        val r = PhotoParsing.parseFxTwitter(json)
        assertEquals(listOf(DirectItem("https://video.twimg.com/v.mp4", true)), r.items)
        assertEquals("https://pbs.twimg.com/t.jpg", r.thumbnail)
    }

    @Test fun fxTwitterWithoutMedia() {
        assertEquals(0, PhotoParsing.parseFxTwitter("""{"code":404,"message":"NOT_FOUND"}""").items.size)
    }

    // Regression: v1 saved a Reel's cover image as if it were a photo.
    @Test fun reelLinksAreVideos() {
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.instagram.com/reel/C1abc/?igsh=x"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.instagram.com/reels/C1abc/"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.instagram.com/someone/reel/C1abc/"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.instagram.com/tv/C1abc/"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.facebook.com/watch/?v=1"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://www.facebook.com/page/videos/123/"))
        assertTrue(PhotoParsing.urlLooksLikeVideo("https://fb.watch/abc/"))
        assertFalse(PhotoParsing.urlLooksLikeVideo("https://www.instagram.com/p/C1abc/"))
        assertFalse(PhotoParsing.urlLooksLikeVideo("https://www.facebook.com/photo/?fbid=1"))
    }

    @Test fun videoPagesAreDetectedFromTags() {
        assertTrue(PhotoParsing.isVideoPage("""<meta property="og:video" content="https://v.mp4">"""))
        assertTrue(PhotoParsing.isVideoPage("""<meta property="og:type" content="video.other">"""))
        assertFalse(PhotoParsing.isVideoPage("""<meta property="og:type" content="article"><meta property="og:image" content="https://a.jpg">"""))
    }

    @Test fun readsOpenGraphInAnyAttributeOrder() {
        val html = """
            <html><head>
            <meta property="og:title" content="Photo by Ali &amp; Sara" />
            <meta content="https://scontent.cdn/p1.jpg?a=1&amp;b=2" property="og:image">
            <meta property='og:image' content='https://scontent.cdn/p1.jpg?a=1&amp;b=2'>
            <meta name="og:image" content="https://scontent.cdn/p2.jpg">
            <meta property="og:image:width" content="1080">
            </head></html>
        """.trimIndent()
        assertEquals(
            listOf("https://scontent.cdn/p1.jpg?a=1&b=2", "https://scontent.cdn/p2.jpg"),
            PhotoParsing.ogImages(html),
        )
        assertEquals("Photo by Ali & Sara", PhotoParsing.ogTitle(html))
    }
}
