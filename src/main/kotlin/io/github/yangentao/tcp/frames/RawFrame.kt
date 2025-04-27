@file:Suppress("unused")

package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

class RawFrame(override val maxFrameLength: Int = 2048) : NetFrame {

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        return buf.size to ByteArray(buf.size) { buf[it] }
    }
}
