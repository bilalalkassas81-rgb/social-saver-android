package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkValidatorTest {
    private fun valid(text: String) = LinkValidator.check(text) as LinkCheck.Valid

    @Test fun detectsInstagram() {
        val r = valid("https://www.instagram.com/reel/C1abcDEF/?igsh=xyz")
        assertEquals(Platform.INSTAGRAM, r.platform)
        assertEquals("https://www.instagram.com/reel/C1abcDEF/?igsh=xyz", r.url)
    }

    @Test fun detectsFacebookVariants() {
        assertEquals(Platform.FACEBOOK, valid("https://www.facebook.com/watch/?v=123").platform)
        assertEquals(Platform.FACEBOOK, valid("https://m.facebook.com/story.php?id=1").platform)
        assertEquals(Platform.FACEBOOK, valid("https://fb.watch/abcd/").platform)
    }

    @Test fun detectsX() {
        assertEquals(Platform.X, valid("https://x.com/user/status/1790000000000000000").platform)
        assertEquals(Platform.X, valid("https://twitter.com/user/status/1").platform)
        assertEquals(Platform.X, valid("https://mobile.twitter.com/user/status/1").platform)
    }

    @Test fun extractsLinkFromSharedText() {
        val r = valid("Check this out! https://www.instagram.com/p/ABC123/ so funny.")
        assertEquals("https://www.instagram.com/p/ABC123/", r.url)
    }

    @Test fun trimsTrailingPunctuation() {
        assertEquals("https://x.com/a/status/5", valid("(https://x.com/a/status/5).").url)
    }

    @Test fun lookalikeDomainIsNotInstagram() {
        assertEquals(Platform.OTHER, valid("https://notinstagram.com/p/1").platform)
    }

    @Test fun rejectsEmptyAndNonLinks() {
        assertTrue(LinkValidator.check("   ") is LinkCheck.Invalid)
        assertTrue(LinkValidator.check("instagram reel") is LinkCheck.Invalid)
        assertTrue(LinkValidator.check("https://localhost") is LinkCheck.Invalid)
    }
}
