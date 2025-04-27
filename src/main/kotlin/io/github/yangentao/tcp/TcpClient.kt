package io.github.yangentao.tcp

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.ClosedSelectorException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

class TcpClient(val bufferFrame: NetFrame, val host: String, val port: Int) {
    val thread: Thread by lazy { Thread(::runLoop, "tcp_client_loop").apply { this.isDaemon = true } }
    private var selector: Selector = Selector.open()
    private var selectionKey: SelectionKey? = null
    private val channel: SocketChannel? get() = selectionKey?.socketChannel
    private lateinit var tcpCallback: TcpClientCallback

    //读超时时间
    var readIdleSeconds: Int = 90

    val isOpen: Boolean get() = selector.isOpen

    val isActive: Boolean
        get() {
            val ch = channel ?: return false
            return ch.isOpen && ch.isConnected
        }

    fun write(data: ByteArray): Boolean {
        return selectionKey?.write(data) ?: false
    }

    @Synchronized
    fun start(callback: TcpClientCallback): Boolean {
        if (isActive) {
            throw IllegalStateException("已存在是start状态")
        }
        this.tcpCallback = callback
        thread.start()
        val ch = SocketChannel.open()
        ch.configureBlocking(false)
        ch.connect(InetSocketAddress(host, port))
        val key = ch.register(selector, SelectionKey.OP_CONNECT)
        key.framer = bufferFrame
        key.callback = this.tcpCallback
        this.selectionKey = key
        selector.wakeup()
        return true
    }

    @Synchronized
    fun stop() {
        selectionKey?.close()
        selectionKey = null
        this.selector.close()
    }

    fun waitThreadExit(millSeconds: Long) {
        this.thread.join(millSeconds)
    }

    private fun runLoop() {
        val sel = this.selector
        while (sel.isOpen) {
            checkIdle()
            try {
                if (sel.select(2000) == 0) {
                    continue
                }
            } catch (ex: ClosedSelectorException) {
                break
            } catch (ioe: IOException) {
                sel.close()
                break
            }
            val ite = sel.selectedKeys().iterator()
            if (!ite.hasNext()) continue
            val key = ite.next()
            ite.remove()
            if (!key.isValid) {
                break
            }
            try {
                if (key.isConnectable) {
                    val ch = key.socketChannel
                    if (ch.isConnectionPending && ch.finishConnect()) {
                        key.interestOps(SelectionKey.OP_READ)
                        key.readTime = System.currentTimeMillis()
                        tcpCallback.onTcpConnect(key, true)
                    } else {
                        tcpCallback.onTcpConnect(key, false)
                        break
                    }
                } else if (key.isReadable) {
                    key.readTime = System.currentTimeMillis()
                    if (!key.isValid) {
                        break
                    }
                    val buf = key.readBuffer() ?: break
                    key.checkFrame(buf, bufferFrame, tcpCallback)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
        selectionKey?.close()
        sel.close()
        selectionKey = null
    }

    private fun checkIdle() {
        val key = selectionKey ?: return
        val tm = System.currentTimeMillis() - this.readIdleSeconds * 1000L
        if (key.readTime != 0L && key.readTime < tm) {
            tcpCallback.onTcpIdle(key)
        }

    }
}