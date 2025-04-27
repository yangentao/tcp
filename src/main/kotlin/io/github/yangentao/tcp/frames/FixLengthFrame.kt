package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

class FixLengthFrame(val length: Int, override val maxFrameLength: Int = 2048) : NetFrame {

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        if (buf.size >= length) {
            return length to buf.sliceArray(0 until length)
        }
        return 0 to null
    }
}