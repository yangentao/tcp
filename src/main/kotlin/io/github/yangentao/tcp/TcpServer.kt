@file:Suppress("unused")

package io.github.yangentao.tcp

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.ClosedSelectorException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

/**
 * tcp server
 */
class TcpServer(
    private val bufferFrame: NetFrame,
    private val maxClient: Int = 2048,
    private val backlog: Int = 128
) {
    private var serianNO: Long = 0
    private lateinit var serverCallback: TcpServerCallback
    private var selector: Selector = Selector.open()
    private var serverSocket: ServerSocketChannel? = null
    private var acceptKey: SelectionKey? = null
    val thread: Thread by lazy { Thread(::runLoop, "tcp_server_loop").apply { this.isDaemon = true } }

    var clientIdleSeconds: Int = 90

    val clientCount: Int get() = clientKeys.size

    val clientKeys: List<SelectionKey>
        get() {
            return try {
                if (selector.isOpen) {
                    selector.keys()?.filter { it.isValid && it.interestRead } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    val isOpen: Boolean
        get() = this.serverSocket?.isOpen ?: false

    @Synchronized
    fun start(port: Int, callback: TcpServerCallback) {
        if (isOpen) throw IllegalStateException("已存在是start状态")
        this.serverCallback = callback
        val ch = ServerSocketChannel.open()
        ch.configureBlocking(false)
        try {
            ch.socket().bind(InetSocketAddress(port), backlog)
        } catch (ex: Exception) {
            ex.printStackTrace()
            ch.close()
            selector.close()
            throw ex
        }
        this.serverSocket = ch
        this.acceptKey = ch.register(selector, SelectionKey.OP_ACCEPT)
        thread.start()
    }

    @Synchronized
    fun stop() {
        closeAllClient()
        acceptKey?.cancel()
        serverSocket?.close()
        selector.close()
        serverSocket = null
        acceptKey = null
    }

    fun waitThreadExit(millSeconds: Long) {
        this.thread.join(millSeconds)
    }

    private fun checkOnce(sel: Selector): Boolean {
        try {
            checkIdle()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            if (sel.select(2000) == 0) {
                return true
            }
        } catch (ex: ClosedSelectorException) {
            return false
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            return false
        }
        val ite = sel.selectedKeys().iterator()
        while (ite.hasNext()) {
            val key = ite.next()
            ite.remove()
            if (!key.isValid) {
                key.close()
                continue
            }
            if (key.isAcceptable) {
                acceptKey(sel, key)
                continue
            }
            if (key.isReadable) {
                readKey(key)
                continue
            }
        }
        return true
    }

    private fun runLoop() {
        val sel = this.selector
        while (sel.isOpen && checkOnce(sel)) {
            tcpLoggerDefault.flush()
        }
        acceptKey?.cancel()
        closeAllClient()
        serverSocket?.close()
        selector.close()
        serverSocket = null
        acceptKey = null
        tcpLoggerDefault.flush()
    }

    private fun checkIdle() {
        if (clientIdleSeconds <= 0) return
        val tm = System.currentTimeMillis() - clientIdleSeconds * 1000
        val ls = ArrayList<SelectionKey>()
        val all = clientKeys
        for (a in all) {
            if (a.readTime != 0L && a.readTime < tm) {
                ls += a
            }
        }
        for (k in ls) {
            try {
                serverCallback.onTcpIdle(k)
            } catch (ex: Exception) {
                logTcp(ex.stackInfo)
                ex.printStackTrace()
            }
        }
    }

    //TODO 达到maxClient时， 取消ServerKey的注册， 当有链接断开后， 再继续监听
    private fun acceptKey(sel: Selector, key: SelectionKey) {
        if (clientCount >= maxClient) {
            key.close()
            return
        }
        val svr = key.serverChannel
        val client = svr.accept()//ex
        client.configureBlocking(false)
        val clientKey = client.register(sel, SelectionKey.OP_READ)
        clientKey.framer = this.bufferFrame
        clientKey.readTime = System.currentTimeMillis()
        clientKey.callback = serverCallback
        serianNO += 1
        clientKey.serialNO = serianNO
        try {
            serverCallback.onTcpAccept(clientKey)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun closeAllClient() {
        try {
            val all = clientKeys
            for (k in all) {
                k.close()
            }
        } catch (ex: Exception) {
        }
    }

    private fun readKey(key: SelectionKey) {
        key.readTime = System.currentTimeMillis()
        val buf = key.readBuffer()
        if (buf == null) {
            key.close()
            return
        }
        try {
            if (serverCallback.onTcpRecvData(key, buf)) return
        } catch (ex: Exception) {
            logTcp(ex.stackInfo)
            ex.printStackTrace()
        }
        key.checkFrame(buf, bufferFrame, serverCallback)
    }
}
