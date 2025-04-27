package io.github.yangentao.tcp

import java.io.PrintWriter
import java.io.StringWriter

internal val ByteArray.strUTF8: String get() = String(this, Charsets.UTF_8)

internal val Throwable.stackInfo: String
    get() {
        val w = StringWriter(1024)
        val p = PrintWriter(w)
        this.printStackTrace(p)
        p.flush()
        return w.toString()
    }