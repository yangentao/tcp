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

fun Int.toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
    val b0: Byte = ((this shr 24) and 0x00ff).toByte()
    val b1: Byte = ((this shr 16) and 0x00ff).toByte()
    val b2: Byte = ((this shr 8) and 0x00ff).toByte()
    val b3: Byte = (this and 0x00ff).toByte()
    return if (order == ByteOrder.BIG_ENDIAN) {
        byteArrayOf(b0, b1, b2, b3)
    } else {
        byteArrayOf(b3, b2, b1, b0)
    }
}