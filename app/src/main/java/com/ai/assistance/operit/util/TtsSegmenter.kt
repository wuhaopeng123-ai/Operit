package com.ai.assistance.operit.util

/**
 * 把连续文本切成适合 TTS 逐段合成的片段。
 *
 * 切分只允许落在「真实句末」上；URL、时间、版本号、日期、路径、IP 等
 * 内部的分隔符（`.` `:` `/`）一律不算句末，避免把一条网址切成四段、
 * 把 17:28:12 切成三段，从而让合成端每段都要等一次完整往返。
 */
object TtsSegmenter {
    const val MAX_SEGMENT_LENGTH = 50
    const val END_CHARS = "!?;:。！？；：\n"

    /** URL 允许出现的字符（RFC 3986 + 常见收尾），中文/空格到这里就断，避免吞掉后文。 */
    private const val URL_CHARS =
        "A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%"

    private val PROTECTED_PATTERNS = listOf(
        // http(s)://...  ftp://...  www....
        Regex("(?i)(?:https?|ftp)://[$URL_CHARS]+"),
        Regex("(?i)www\\.[$URL_CHARS]+"),
        // 17:28 / 17:28:12 / 17:28:12.345
        Regex("\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\.\\d+)?"),
        // 1.2 / 1.2.3
        Regex("\\d+\\.\\d+(?:\\.\\d+)*"),
        // 2026-09-25 / 2026/9/25
        Regex("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}"),
        // 192.168.3.5:22
        Regex("\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?"),
        // /sdcard/Download/a.txt · com/ai/assistance
        Regex("[A-Za-z0-9_\\-]+(?:/[A-Za-z0-9_\\-]+)+"),
        // #1234 / v1.2.3
        Regex("(?i)v?\\d+(?:\\.\\d+)+")
    )

    /** 扫描一次，标出所有「不许切」的区间，并做合并排序。 */
    private fun protectedRanges(text: CharSequence): List<IntRange> {
        val raw = ArrayList<IntRange>(16)
        PROTECTED_PATTERNS.forEach { pattern ->
            pattern.findAll(text).forEach { m ->
                if (m.value.isNotEmpty()) raw.add(m.range)
            }
        }
        // 数字本身也是保护区：任何紧邻数字的位置都不许当句末
        for (i in text.indices) {
            if (text[i].isDigit()) raw.add(i..i)
        }
        if (raw.isEmpty()) return emptyList()
        raw.sortBy { it.first }
        val merged = ArrayList<IntRange>(raw.size)
        var cur = raw[0]
        for (r in raw.drop(1)) {
            if (r.first <= cur.last + 1) {
                if (r.last > cur.last) cur = cur.first..r.last
            } else {
                merged.add(cur)
                cur = r
            }
        }
        merged.add(cur)
        return merged
    }

    private fun isProtected(ranges: List<IntRange>, index: Int): Boolean {
        var lo = 0
        var hi = ranges.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val r = ranges[mid]
            when {
                index < r.first -> hi = mid - 1
                index > r.last -> lo = mid + 1
                else -> return true
            }
        }
        return false
    }

    fun findFirstEndCharIndex(text: CharSequence): Int {
        val ranges = protectedRanges(text)
        for (index in 0 until text.length) {
            if (isProtected(ranges, index)) continue
            if (isSegmentEndingChar(text, index)) return index
        }
        return -1
    }

    fun nextSegmentEnd(buffer: CharSequence): Int {
        val endIndex = findFirstEndCharIndex(buffer)
        if (endIndex >= 0) {
            var boundary = endIndex + 1
            while (boundary < buffer.length && isTrailingEndingChar(buffer, boundary)) {
                boundary++
            }
            return boundary
        }
        // 没有句末：只有当尾巴确实过长、且不在保护区中间时才硬切
        if (buffer.length >= MAX_SEGMENT_LENGTH) {
            val ranges = protectedRanges(buffer)
            var cut = buffer.length
            if (isProtected(ranges, cut - 1)) {
                cut = -1
                for (i in buffer.length - 1 downTo 1) {
                    if (!isProtected(ranges, i)) {
                        cut = i
                        break
                    }
                }
                if (cut < 0) return -1
            }
            return cut
        }
        return -1
    }

    fun split(text: String): List<String> {
        val buffer = StringBuilder(text)
        val segments = mutableListOf<String>()

        while (buffer.isNotEmpty()) {
            val endIndex = nextSegmentEnd(buffer)
            if (endIndex < 0) break

            val segment = buffer.substring(0, endIndex).trim()
            if (segment.isNotEmpty()) segments += segment
            buffer.delete(0, endIndex)
        }

        val remaining = buffer.toString().trim()
        if (remaining.isNotEmpty()) segments += remaining
        return segments
    }

    private fun isSegmentEndingChar(text: CharSequence, index: Int): Boolean {
        val current = text[index]
        if (END_CHARS.indexOf(current) >= 0) {
            return true
        }

        if (current != '.') {
            return false
        }

        val next = text.getOrNull(index + 1)
        return next == null || (!next.isDigit() && next != '.')
    }

    private fun isTrailingEndingChar(text: CharSequence, index: Int): Boolean {
        val current = text[index]
        if (END_CHARS.indexOf(current) >= 0) {
            return true
        }

        if (current != '.') {
            return false
        }

        val previous = text.getOrNull(index - 1)
        return previous == '.'
    }

    private fun CharSequence.getOrNull(index: Int): Char? {
        if (index < 0 || index >= length) {
            return null
        }
        return this[index]
    }
}
