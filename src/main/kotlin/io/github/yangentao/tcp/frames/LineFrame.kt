package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

@Suppress("PrivatePropertyName")
class LineFrame(override val maxFrameLength: Int = 2048) : NetFrame {
    private val CR: Byte = 13
    private val LF: Byte = 10

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        for (i in buf.indices) {
            if (buf[i] == CR || buf[i] == LF) {
                val data = buf.sliceArray(0 until i)
                var k = i + 1
                while (k < buf.size) {
                    if (buf[k] == CR || buf[k] == LF) {
                        ++k
                    } else {
                        break
                    }
                }
                return k to data
            }
        }
        return 0 to null
    }

}
