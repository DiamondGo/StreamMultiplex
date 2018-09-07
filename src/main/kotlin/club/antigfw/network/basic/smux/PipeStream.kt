package club.antigfw.network.basic.smux

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ClosedReceiveChannelException
import kotlinx.coroutines.experimental.channels.ClosedSendChannelException
import kotlinx.coroutines.experimental.runBlocking
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

class PipeStream {
    companion object {
        private const val bufSize = 1024
    }

    private val byteChannel = Channel<Byte>(bufSize)


    fun getInput() : InputStream {
        return object : InputStream() {
            override fun read(): Int {
                return try {
                    val b = runBlocking<Byte> { byteChannel.receive() }
                    b.toInt() and 0xff
                } catch (e : Exception) {
                    when(e) {
                        is ClosedReceiveChannelException -> -1
                        else -> -1
                    }
                }
            }
        }
    }

    fun getOutput() : OutputStream {
        return object : OutputStream() {
            override fun write(b: Int) {
                try {
                    runBlocking<Unit> { byteChannel.send(b.toByte()) }
                } catch (e : ClosedSendChannelException) {
                    throw IOException("StreamOld closed")
                }
            }

            override fun close() {
                byteChannel.close()
                super.close()
            }
        }
    }
}