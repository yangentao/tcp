@file:Suppress("unused")

package io.github.yangentao.tcp

import java.net.StandardSocketOptions
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.reflect.KProperty

/**
 * base frame of bytes
 */
interface NetFrame {
    val maxFrameLength: Int
    fun accept(buf: ByteArray): Pair<Int, ByteArray?>
}

interface TcpLogger {
    fun log(msg: String)
    fun flush() {}
}

internal var tcpLoggerDefault: TcpLogger = object : TcpLogger {
    override fun log(msg: String) {
        println(msg)
    }

    override fun flush() {
    }

}

fun setTcpLogger(logger: TcpLogger) {
    tcpLoggerDefault = logger
}

internal fun logTcp(msg: String) {
    tcpLoggerDefault.log(msg)
}

interface TcpCallback {
    fun onTcpClosed(key: SelectionKey) {}
    fun onTcpIdle(key: SelectionKey) {
        key.close()
    }

    fun onTcpException(key: SelectionKey, ex: Throwable) {
    }

    fun onTcpRecvData(key: SelectionKey, data: ByteArray): Boolean {
        return false
    }

    fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {}
    fun onTcpBadFrame(key: SelectionKey, data: ByteArray) {}
}

interface TcpServerCallback : TcpCallback {
    fun onTcpAccept(key: SelectionKey) {}
}

interface TcpClientCallback : TcpCallback {
    fun onTcpConnect(key: SelectionKey, success: Boolean) {}
}

fun SocketChannel.tcpNoDelay(b: Boolean) {
    this.setOption(StandardSocketOptions.TCP_NODELAY, b)
}

/**
 * count of clients
 */
val Selector.keyCount: Int get() = if (this.isOpen) this.keys().count() else 0
val SelectionKey.socketChannel: SocketChannel get() = this.channel() as SocketChannel
val SelectionKey.serverChannel: ServerSocketChannel get() = this.channel() as ServerSocketChannel

val SelectionKey.interestRead: Boolean get() = 0 != (this.interestOps() and SelectionKey.OP_READ)
val SelectionKey.interestConnect: Boolean get() = 0 != (this.interestOps() and SelectionKey.OP_CONNECT)
val SelectionKey.interestAccept: Boolean get() = 0 != (this.interestOps() and SelectionKey.OP_ACCEPT)

@Suppress("UNCHECKED_CAST")
val SelectionKey.attrMap: HashMap<String, Any>
    get() {
        val m = this.attachment()
        if (m is HashMap<*, *>) {
            return m as HashMap<String, Any>
        }
        val map = HashMap<String, Any>()
        this.attach(map)
        return map
    }

fun SelectionKey.attr(key: String, value: Any?) {
    if (value == null) {
        this.attrMap.remove(key)
    } else {
        this.attrMap[key] = value
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> SelectionKey.attr(key: String, initCallback: () -> T): T {
    return this.attrMap.getOrPut(key, initCallback) as T
}

fun SelectionKey.attr(key: String): Any? {
    return this.attrMap[key]
}

object SelectionKeyValue {
    inline operator fun <reified T : Any> getValue(thisRef: SelectionKey, property: KProperty<*>): T? {
        return thisRef.attrMap[property.name] as? T
    }

    inline operator fun <reified T : Any> setValue(thisRef: SelectionKey, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.attrMap.remove(property.name)
        } else {
            thisRef.attrMap[property.name] = value
        }
    }
}

class SelectionKeyValueDefault(val defaultValue: Any) {
    inline operator fun <reified T : Any> getValue(thisRef: SelectionKey, property: KProperty<*>): T {
        return (thisRef.attrMap[property.name] as? T) ?: (defaultValue as T)
    }

    inline operator fun <reified T : Any> setValue(thisRef: SelectionKey, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.attrMap.remove(property.name)
        } else {
            thisRef.attrMap[property.name] = value
        }
    }
}


