package com.socialsaver.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateParsingTest {
    private fun release(tag: String, prerelease: Boolean = false) = """{
        "tag_name":"$tag","prerelease":$prerelease,"draft":false,
        "html_url":"https://github.com/o/r/releases/tag/$tag",
        "assets":[
          {"name":"SocialSaver-arm64.apk","size":42000000,"browser_download_url":"https://dl/arm64.apk"},
          {"name":"SocialSaver-armv7.apk","size":40000000,"browser_download_url":"https://dl/armv7.apk"}]}"""

    private val arm64Phone = listOf("arm64-v8a", "armeabi-v7a", "armeabi")
    private val oldPhone = listOf("armeabi-v7a", "armeabi")

    @Test fun newerBuildIsOffered() {
        val u = UpdateParsing.parseLatest(release("build-6"), currentVersionCode = 5, abis = arm64Phone)!!
        assertEquals(6, u.versionCode)
        assertEquals("1.0.6", u.versionName)
        assertEquals("https://dl/arm64.apk", u.apkUrl)
        assertEquals(42000000L, u.sizeBytes)
        assertEquals("https://github.com/o/r/releases/tag/build-6", u.pageUrl)
    }

    @Test fun oldPhonesGetArmv7() {
        assertEquals("https://dl/armv7.apk", UpdateParsing.parseLatest(release("build-6"), 5, oldPhone)!!.apkUrl)
    }

    @Test fun sameOrOlderBuildIsIgnored() {
        assertNull(UpdateParsing.parseLatest(release("build-5"), 5, arm64Phone))
        assertNull(UpdateParsing.parseLatest(release("build-3"), 5, arm64Phone))
    }

    @Test fun failedAndOddReleasesAreIgnored() {
        assertNull(UpdateParsing.parseLatest(release("failed-9", prerelease = true), 5, arm64Phone))
        assertNull(UpdateParsing.parseLatest(release("build-9", prerelease = true), 5, arm64Phone))
        assertNull(UpdateParsing.parseLatest(release("v2.0"), 5, arm64Phone))
        assertNull(UpdateParsing.parseLatest("""{"tag_name":"build-9","assets":[]}""", 5, arm64Phone))
    }
}
