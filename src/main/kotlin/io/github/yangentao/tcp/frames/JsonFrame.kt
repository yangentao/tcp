package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame

private const val NL: Byte = 0
private const val LK: Byte = 123 // {
private const val RK: Byte = 125 // }
private const val QT: Byte = 34  // "
private const val ES: Byte = 92  // \
private const val SP: Byte = 32  // 空格
private const val CR: Byte = 13  // CR
private const val LF: Byte = 10  // LF
private const val TB: Byte = 9   // TAB
private val witeSpaces: Set<Byte> = setOf(SP, CR, LF, TB)

/**
 * 只支持utf8 或 ascii
 */

class JsonObjectFrame(val trim: Boolean = true, override val maxFrameLength: Int = 2048) : NetFrame {

    override fun accept(buf: ByteArray): Pair<Int, ByteArray?> {
        if (buf.isEmpty()) {
            return 0 to null
        }
        var fromIndex = 0
        if (trim) {
            while (fromIndex < buf.size && (buf[fromIndex] == NL || (buf[fromIndex] in witeSpaces))) {
                fromIndex += 1
            }
        }
        if (fromIndex >= buf.size) {
            return 0 to null
        }
        if (buf[fromIndex] != LK) {
            return 0 to null
        }
        var lkCount = 1
        var escaping = false
        var inString = false
        for (i in fromIndex + 1 until buf.size) {
            if (inString) {
                if (escaping) {
                    escaping = false
                } else if (buf[i] == QT) {
                    inString = false
                } else if (buf[i] == ES) {
                    escaping = true
                }
                continue
            }
            when (buf[i]) {
                QT -> inString = true
                LK -> lkCount += 1
                RK -> lkCount -= 1
            }
            if (lkCount == 0) {
                return (i + 1) to buf.sliceArray(fromIndex..i)
            }
        }
        return 0 to null
    }

}
