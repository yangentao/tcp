@file:Suppress("unused")

package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame
import java.nio.ByteOrder

class SizeFrame(
    private val byteSize: Int = 4,
    private val order: ByteOrder = ByteOrder.BIG_ENDIAN,
    override val maxFrameLength: Int = 2048
) : NetFrame {

    init {
        assert(byteSize in 1..4)
    }

    private fun parseSize(buf: ByteArray): Int {
        var n = 0
        if (order == ByteOrder.BIG_ENDIAN) {
            for (i in 0 until byteSize) {
                val v = buf[i].toInt() and 0x00ff
                n = (n shl 8) or v
            }
        } else {
            for (i in 0 until byteSize) {
                val v = (buf[i].toInt() and 0x00ff)
                n = n or (v shl (i * 8))
            }
        }
        return n
    }

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        if (buf.size < byteSize) {
            return 0 to null
        }
        val sz = parseSize(buf)
        if (sz == 0) {
            return byteSize to null
        }

        val n = byteSize + sz
        if (buf.size >= n) {
            return n to buf.sliceArray(byteSize until n)
        }
        return 0 to null
    }
}

