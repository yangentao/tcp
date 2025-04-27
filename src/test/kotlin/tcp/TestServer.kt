package tcp

import io.github.yangentao.tcp.TcpServer
import io.github.yangentao.tcp.TcpServerCallback
import io.github.yangentao.tcp.close
import io.github.yangentao.tcp.frames.LineFrame
import io.github.yangentao.tcp.keyCount
import io.github.yangentao.tcp.serialNO
import io.github.yangentao.tcp.strUTF8
import io.github.yangentao.tcp.writeText
import java.nio.channels.SelectionKey

fun main() {
    val callback = object : TcpServerCallback {
        override fun onTcpAccept(key: SelectionKey) {
            println()
            println("-------------------------------")
            println("ACCEPT ${key.serialNO},  total: ${key.selector().keyCount}")
        }

        override fun onTcpClosed(key: SelectionKey) {
            println("CLOSE  ${key.serialNO},  total:  ${key.selector().keyCount}")
        }

        override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
            println("RECV ${key.serialNO},  total:  ${key.selector().keyCount} : " + data.strUTF8)
            key.writeText("echo ${key.serialNO}: " + data.strUTF8)
        }

        override fun onTcpIdle(key: SelectionKey) {
            println("IDLE ${key.serialNO} ")
            key.close()
        }
    }
    val sv = TcpServer(LineFrame())
    sv.clientIdleSeconds = 5
    sv.start(9999, callback)
    sv.thread.join()
    println("Server END")
}