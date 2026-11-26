package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorTextTest {
    @Test fun loginRequired() {
        val msg = ErrorText.friendly("ERROR: [Instagram] C1abc: Requested content is not available, rate-limit reached or login required")
        assertTrue(msg.startsWith("This post needs a login"))
    }

    @Test fun unsupported() {
        assertTrue(ErrorText.friendly("ERROR: Unsupported URL: https://example.com").startsWith("This link isn't supported"))
    }

    @Test fun noVideo() {
        assertEquals("No video or photo was found in this post.", ErrorText.friendly("ERROR: [twitter] 1: No video could be found in this tweet"))
    }

    @Test fun network() {
        assertTrue(ErrorText.friendly("ERROR: Unable to download webpage: timed out").startsWith("Network problem"))
    }

    @Test fun stripsPrefixFromUnknownErrors() {
        assertEquals("Something odd happened", ErrorText.friendly("WARNING: x\nERROR: [facebook] 123: Something odd happened"))
    }

    @Test fun emptyInput() {
        assertEquals("Something went wrong. Please try again.", ErrorText.friendly(null))
    }
}
