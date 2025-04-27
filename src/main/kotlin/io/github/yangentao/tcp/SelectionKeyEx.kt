package io.github.yangentao.tcp

import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

fun SelectionKey.close() {
    if (!this.isValid) return
    this.cancel()
    this.channel().close()
    this.callback?.onTcpClosed(this)
    this.attach(null)

}

var SelectionKey.callback: TcpCallback? by SelectionKeyValue
var SelectionKey.framer: NetFrame? by SelectionKeyValue
var SelectionKey.byteArray: ByteArray? by SelectionKeyValue
var SelectionKey.serialNO: Long by SelectionKeyValueDefault(0L)
var SelectionKey.readTime: Long by SelectionKeyValueDefault(0L)
var SelectionKey.userId: String? by SelectionKeyValue
var SelectionKey.ident: String? by SelectionKeyValue

fun SelectionKey.writeText(text: String, charset: Charset = Charsets.UTF_8): Boolean {
    return this.write(text.toByteArray(charset))
}

fun SelectionKey.write(data: ByteArray): Boolean {
    if (!this.isValid) {
        return false
    }
    val ch = this.channel() as SocketChannel
    val b = ByteBuffer.wrap(data)
    while (b.hasRemaining()) {
        val n = ch.write(b)
        if (n < 0) {
            this.close()
            return false
        } else if (n == 0) {//缓冲区满
            Thread.sleep(10)
        } else {
            continue
        }
    }
    return true
}

private val localReadBuf: ThreadLocal<ByteBuffer> by lazy { ThreadLocal.withInitial { ByteBuffer.allocate(8192) } }

//if return null, channel should be close
fun SelectionKey.readBuffer(): ByteArray? {
    val buf = localReadBuf.get()
    buf.clear()
    val key = this
    val ch = key.channel() as SocketChannel
    var readData: ByteArray = ByteArray(0)
    do {
        val nRead = try {
            ch.read(buf)
        } catch (ex: Exception) {
            key.close()
            ex.printStackTrace()
            return null
        }
        if (nRead == -1) {
            return null
        }
        if (nRead == 0) {
            break
        }
        if (nRead > 0) {
            buf.flip()
            val ba = ByteArray(nRead) { 0 }
            buf.get(ba, 0, nRead)
            readData += ba
        }
    } while (nRead > 0)
    return readData
}

fun SelectionKey.checkFrame(bufRecv: ByteArray, bufferFrame: NetFrame, callback: TcpCallback?) {
    val key: SelectionKey = this
    val oldBuf = key.byteArray
    key.byteArray = null
    val totalBuf = if (oldBuf == null) bufRecv else oldBuf + bufRecv
    val p: Pair<Int, ByteArray?> = bufferFrame.accept(totalBuf)
    val sz = p.first
    if (sz > 0) {
        try {
            callback?.onTcpRecvFrame(key, p.second!!)
        } catch (ex: Exception) {
            logTcp(ex.stackInfo)
        }
        if (sz < totalBuf.size) {
            this.checkFrame(totalBuf.sliceArray(sz until totalBuf.size), bufferFrame, callback)
        }
    } else {
        if (totalBuf.size > bufferFrame.maxFrameLength) {
            try {
                callback?.onTcpBadFrame(key, totalBuf)
            } catch (ex: Exception) {
                logTcp(ex.stackInfo)
            }
        } else {
            key.byteArray = totalBuf
        }
    }
}
