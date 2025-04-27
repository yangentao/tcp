@file:Suppress("unused")

package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

class ListFrame(private val frameList: List<NetFrame>, override val maxFrameLength: Int = 2048) : NetFrame {

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        for (f in frameList) {
            val p = f.accept(buf)
            if (p.first > 0 && p.second != null) return p
        }
        return 0 to null
    }

}