package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

class EndEdgeFrame(private val end: ByteArray, override val maxFrameLength: Int = 2048) : NetFrame {

    init {
        assert(end.isNotEmpty())
    }

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        if (buf.size < end.size) {
            return 0 to null
        }

        for (i in buf.indices) {
            if (i + end.size <= buf.size) {
                var acceptEnd = true
                for (k in end.indices) {
                    if (end[k] != buf[i + k]) {
                        acceptEnd = false
                        break
                    }
                }
                if (acceptEnd) {
                    return (i + end.size) to buf.sliceArray(0 until i)
                }
            }
        }
        return 0 to null
    }
}