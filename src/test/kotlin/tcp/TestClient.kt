package tcp

import io.github.yangentao.tcp.TcpClient
import io.github.yangentao.tcp.TcpClientCallback
import io.github.yangentao.tcp.close
import io.github.yangentao.tcp.frames.LineFrame
import io.github.yangentao.tcp.serialNO
import io.github.yangentao.tcp.strUTF8
import io.github.yangentao.tcp.writeText
import java.nio.channels.SelectionKey

val clientLines1 = arrayListOf("1", "2", "3", "4", "5")
val clientLines2 = arrayListOf("11", "22", "33", "44", "55")
val clientLines3 = arrayListOf("111", "222", "333", "444", "555")

fun runClient(lines: ArrayList<String>): TcpClient {
    val c = TcpClient(LineFrame(), "localhost", 9999)
    val callback = object : TcpClientCallback {
        override fun onTcpClosed(key: SelectionKey) {
            println("CLOSE ${key.serialNO}")
        }

        override fun onTcpRecvFrame(key: SelectionKey, data: ByteArray) {
            println("RECV ${key.serialNO}: " + data.strUTF8)
            val line = lines.removeFirstOrNull()
            if (line != null) {
                if (line == "QUIT") {
                    key.close()
                    key.selector().close()
                } else {
                    key.writeText(line)
//                    Thread.sleep(1000)
                }
            } else {
                //wait idle
            }
        }

        override fun onTcpIdle(key: SelectionKey) {
            println("IDLE ${key.serialNO} ")
            c.stop()
        }

        override fun onTcpConnect(key: SelectionKey, success: Boolean) {
            if (success) {
                println("CONNECT SUCCESS ${key.serialNO}")
                key.writeText("OK")
            } else {
                println("CONNECT FAILED ")
            }
        }
    }

    c.readIdleSeconds = 50
    c.start(callback)

    return c
}

fun main() {
    val start = System.currentTimeMillis()
    val ls = ArrayList<Thread>()
//    runClient(arrayListOf("1", "2", "3", "4")).thread.join()
    for (i in 0..3) {
        ls += runClient(arrayListOf("1", "2", "QUIT")).thread
    }
    for (a in ls) {
        a.join()
    }
//    runClient(arrayListOf("1", "2", "3", "4", "5")).thread.join()
    val ms = System.currentTimeMillis() - start
    println("mill seconds: $ms ")
    println("CLIENT END")
}