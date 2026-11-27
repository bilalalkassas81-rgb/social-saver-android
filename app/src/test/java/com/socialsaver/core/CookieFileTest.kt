package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieFileTest {
    private val header = "csrftoken=abc; sessionid=123%3Axyz; ds_user_id=9; empty="

    @Test fun parsesBrowserCookieHeader() {
        assertEquals(
            listOf("csrftoken" to "abc", "sessionid" to "123%3Axyz", "ds_user_id" to "9", "empty" to ""),
            CookieFile.parseHeader(header),
        )
        assertTrue(CookieFile.parseHeader(null).isEmpty())
    }

    @Test fun detectsSession() {
        assertTrue(CookieFile.hasCookie(header, "sessionid"))
        assertFalse(CookieFile.hasCookie(header, "empty"))
        assertFalse(CookieFile.hasCookie("csrftoken=abc", "sessionid"))
        assertFalse(CookieFile.hasCookie(null, "sessionid"))
    }

    @Test fun buildsNetscapeFile() {
        val text = CookieFile.build(listOf(CookieFile.lines(".instagram.com", "sessionid=1; mid=2", 1700000000)))
        assertEquals(
            "# Netscape HTTP Cookie File\n\n" +
                ".instagram.com\tTRUE\t/\tTRUE\t1700000000\tsessionid\t1\n" +
                ".instagram.com\tTRUE\t/\tTRUE\t1700000000\tmid\t2\n",
            text,
        )
    }

    @Test fun readsSetCookieHeaders() {
        assertEquals("csrftoken" to "abc", CookieFile.parseSetCookie("csrftoken=abc; Domain=.instagram.com; Path=/; Secure"))
        assertEquals("mid" to "Z1", CookieFile.parseSetCookie("mid=\"Z1\"; Path=/"))
        assertEquals(null, CookieFile.parseSetCookie("sessionid=\"\"; Max-Age=0; Path=/"))
        assertEquals(null, CookieFile.parseSetCookie("x=1; expires=Thu, 01 Jan 1970 00:00:00 GMT"))
        assertEquals(null, CookieFile.parseSetCookie("garbage"))
    }
}
