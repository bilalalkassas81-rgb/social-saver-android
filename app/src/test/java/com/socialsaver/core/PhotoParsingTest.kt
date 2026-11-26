package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(2, r.photos.size)
        assertEquals("https://pbs.twimg.com/media/B.png?name=orig", r.photos[1])
    }

    @Test fun fxTwitterWithoutPhotos() {
        assertEquals(0, PhotoParsing.parseFxTwitter("""{"code":404,"message":"NOT_FOUND"}""").photos.size)
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
