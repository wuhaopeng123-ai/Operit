package com.ai.assistance.operit.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsSegmenterTest {
    @Test
    fun decimalNumber_isNotSplitByDot() {
        assertEquals(listOf("价格是 12.25 元。"), TtsSegmenter.split("价格是 12.25 元。"))
    }

    @Test
    fun versionNumber_isNotSplitByDot() {
        assertEquals(listOf("当前版本 v1.2 已发布。"), TtsSegmenter.split("当前版本 v1.2 已发布。"))
    }

    @Test
    fun sentenceEndingDot_stillSplitsNormally() {
        assertEquals(listOf("第一句.", "第二句"), TtsSegmenter.split("第一句.第二句"))
    }
    @Test
    fun urlWithDots_isNotSplit() {
        assertEquals(
                listOf("入口在 https://open.bigmodel.cn/api/paas/v4/audio/speech 这里。"),
                TtsSegmenter.split("入口在 https://open.bigmodel.cn/api/paas/v4/audio/speech 这里。")
        )
    }

    @Test
    fun timestamp_isNotSplit() {
        assertEquals(
                listOf("现在是17:28:12，服务已经恢复。"),
                TtsSegmenter.split("现在是17:28:12，服务已经恢复。")
        )
    }

    @Test
    fun ipWithPort_isNotSplit() {
        assertEquals(
                listOf("SSH 走 219.128.141.32:22223 这个口。"),
                TtsSegmenter.split("SSH 走 219.128.141.32:22223 这个口。")
        )
    }

    @Test
    fun dateWithDashes_isNotSplit() {
        assertEquals(
                listOf("2026-09-25 这天改的。"),
                TtsSegmenter.split("2026-09-25 这天改的。")
        )
    }

    @Test
    fun unixPath_isNotSplit() {
        assertEquals(
                listOf("文件在 /sdcard/Download/Operit/tmp 下面。"),
                TtsSegmenter.split("文件在 /sdcard/Download/Operit/tmp 下面。")
        )
    }

    @Test
    fun urlAndTimestampTogether_stayOneSegment() {
        val text = "17:28:12 的时候我去看了看 https://open.bigmodel.cn/api/paas/v4/audio/speech 这个接口，返回是正常的。"
        assertEquals(listOf(text), TtsSegmenter.split(text))
    }

    @Test
    fun urlFollowedBySentenceEnd_stillSplits() {
        assertEquals(
                listOf("先去 https://a.b/c 看看。", "下面还有一句。"),
                TtsSegmenter.split("先去 https://a.b/c 看看。下面还有一句。")
        )
    }
}
