package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test fun findsSessionTokenInHomePage() {
        assertEquals("AVq1", InstagramParsing.extractLsd("""...["LSD",[],{"token":"AVq1"},323]..."""))
        assertEquals("AVq2", InstagramParsing.extractLsd("""<script type="application/json" id="__eqmc" data-sjs>{"u":"/x","l":"AVq2","e":"1"}</script>"""))
        assertEquals("AVq3", InstagramParsing.extractLsd("""<input type="hidden" name="lsd" value="AVq3" />"""))
        assertNull(InstagramParsing.extractLsd("<html>login</html>"))
    }

    // The logged-out GraphQL answer has the same media shape as the logged-in API.
    @Test fun parsesLoggedOutGraphqlReel() {
        val json = """for (;;);{"data":{"xig_polaris_media":{"if_not_gated_logged_out":{
            "caption":{"text":"Sunset reel"},"user":{"username":"ali"},
            "video_versions":[
              {"type":101,"url":"https://v/720.mp4","width":720,"height":1280},
              {"type":102,"url":"https://v/480.mp4","width":480,"height":854},
              {"type":103,"url":"https://v/720.mp4","width":720,"height":1280}],
            "image_versions2":{"candidates":[{"url":"https://i/cover.jpg","width":720,"height":1280}]}}}}}"""
        val post = InstagramParsing.parseGraphql(json)!!
        assertEquals("Sunset reel", post.title)
        assertEquals(listOf(DirectItem("https://v/720.mp4", true)), post.items)
        val options = InstagramParsing.options(post)
        assertEquals(listOf("720p", "480p"), options.map { it.label })
        assertEquals(listOf(DirectItem("https://v/480.mp4", true)), options[1].items)
        assertTrue(options.all { it.kind == OptionKind.DIRECT })
    }

    @Test fun gatedGraphqlAnswerGivesNothing() {
        assertNull(InstagramParsing.parseGraphql("""{"data":{"xig_polaris_media":{"if_not_gated_logged_out":null}}}"""))
        assertNull(InstagramParsing.parseGraphql("""{"errors":[{"message":"rate limited"}]}"""))
        assertNull(InstagramParsing.parseGraphql("<html>"))
    }

    @Test fun photoAndCarouselOptions() {
        val photo = InstagramParsing.IgPost("p", null, listOf(DirectItem("https://i/1.jpg", false)))
        assertEquals("Photo, full size", InstagramParsing.options(photo).single().label)
        val mixed = InstagramParsing.IgPost("c", null, listOf(DirectItem("a", false), DirectItem("b", true), DirectItem("c", false)))
        val option = InstagramParsing.options(mixed).single()
        assertEquals("All 3 items", option.label)
        assertEquals(3, option.items.size)
    }

    @Test fun graphqlFormMatchesInstagram() {
        val form = InstagramParsing.graphqlForm("TOKEN", "123")
        assertEquals("TOKEN", form["lsd"])
        assertEquals("""{"media_id":"123"}""", form["variables"])
        assertEquals(InstagramParsing.GRAPHQL_DOC_ID, form["doc_id"])
    }
}
