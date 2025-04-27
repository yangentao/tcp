package io.github.yangentao.tcp.frames

import io.github.yangentao.tcp.NetFrame
import io.github.yangentao.tcp.logTcp
import io.github.yangentao.tcp.strUTF8

//
//fun testFixLength() {
//	val buf = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
//	val f = FixLengthFrame(3)
//	val p = f.accept(buf)
//	logd(p.first, p.second?.joinToString(",") { it.toString() })
//
//
//}
//
fun testFrame(buf: ByteArray, f: NetFrame) {
    val p = f.accept(buf)
    logTcp("size: ${p.first} data:" + p.second?.joinToString(",") { it.toString() })
}

fun testFrames() {
    val buf = byteArrayOf(0, 6, 3, 4, 5, 6, 7, 8, 9, 10, 13, 10, 13, 14)
    testFrame(buf, FixLengthFrame(3))
    testFrame(buf, EndEdgeFrame(byteArrayOf(4, 5)))
    testFrame(buf, EdgeFrame(byteArrayOf(0, 6), byteArrayOf(8, 9)))
    testFrame(buf, LineFrame())
//	testFrame(buf, SizeFrame(1))
    testFrame(buf, SizeFrame(2))
}

//
fun testJsonFrame() {
    val s = """
		{"name":"yang","addr":"hello"} {"age":22}
	""".trimIndent()
    val buf = s.toByteArray()
    val f = JsonObjectFrame()
    val n = f.accept(buf)
    logTcp("${buf.size} ${n.first} " + n.second?.strUTF8)
}

//
//
fun main() {
    testJsonFrame()
    testFrames()
}