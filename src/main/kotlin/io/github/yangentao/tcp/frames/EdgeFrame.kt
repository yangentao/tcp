package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

/**
 * {"name":"yang"}
 * start:  "{"
 * end:  "}"
 * trimEdge, if trim, return "name":"yang",  else return   {"name":"yang"}
 * strict, if true , edge MUST appear at first/last, else start/end edge can appear any position
 */

class EdgeFrame(private val start: ByteArray, private val end: ByteArray, private val trimEdge: Boolean = false, private val strict: Boolean = false, override val maxFrameLength: Int = 2048) : NetFrame {

    init {
        assert(start.isNotEmpty() && end.isNotEmpty())
    }

    private fun match(buf: ByteArray, from: Int, edge: ByteArray): Boolean {
        var bufIndex: Int = 0
        for (i in edge.indices) {
            bufIndex = from + i
            if (bufIndex >= buf.size) return false
            if (buf[bufIndex] != edge[i]) return false
        }
        return true
    }

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        if (buf.size < start.size + end.size) {
            return 0 to null
        }
        var from: Int = 0
        if (strict) {
            if (!match(buf, from, start)) return 0 to null
        } else {
            val beforeEndSize: Int = buf.size - end.size
            while (from < beforeEndSize) {
                if (match(buf, from, start)) break
                from += 1
            }
            if (from >= beforeEndSize) return 0 to null
        }
        val startIndex: Int = from
        from += start.size
        var matchEndIndex: Int = 0
        for (i in from..<buf.size) {
            if (match(buf, i, end)) {
                matchEndIndex = i
                break
            }
        }
        val matchEndSize: Int = matchEndIndex + end.size
        if (matchEndIndex >= startIndex) {
            if (trimEdge) {
                return matchEndSize to buf.sliceArray((startIndex + start.size)..<(matchEndIndex))
            } else {
                return matchEndSize to buf.sliceArray(startIndex..<matchEndSize)
            }
        }
        return 0 to null
    }
}