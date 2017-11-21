package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.selects.select
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeoutException

class Stream(
        val id: Int,
        val session: Session
) {
    private val incomingFrames = Channel<Frame>(session.config.maxStreamFrame)

    private val die = Channel<Any>()

    /***
     * implement read
     */
    fun getInputStream(): InputStream {
        return inStream
    }

    private val inStream = object : InputStream() {
        var currentFrame: Frame? = null
        var readIdx = 0

        override fun read(): Int {
            val ba = ByteArray(1)
            val read = read(ba)
            return if (read == 0) -1 else ba[0].toInt()
        }

        override fun read(b: ByteArray?): Int {
            if (b == null)
                throw java.lang.NullPointerException("input ByteArray is null")
            return read(b, 0, b.size)
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
            if (b == null)
                throw java.lang.NullPointerException("input ByteArray is null")
            if (off < 0 || len < 0 || len > b.size- off)
                throw java.lang.IndexOutOfBoundsException()

            var read = 0
            readLoop@ while (read < len) {
                if (currentFrame == null || readIdx == currentFrame!!.data.size) {
                    try {
                        runBlocking(CommonPool) { getNextFrame() }
                    } catch (ex: java.lang.Exception) {
                        when(ex) {
                            is EOFException -> break@readLoop
                            is TimeoutException -> throw IOException("Read timeout")
                        }
                    }
                }

                val toRead = minOf(currentFrame!!.data.size - readIdx, len - read)
                (0 until toRead).forEach {
                    b[off + read + it] = currentFrame!!.data[readIdx + it]
                }
                readIdx += toRead
                read += toRead
            }

            return read
        }

        suspend private fun getNextFrame() {
            val timeout = Channel<Any>()
            launch {
                delay(this@Stream.session.config.receiveTimeout.toMillis())
                timeout.send(Any())
            }
            select<Unit> {
                die.onReceiveOrNull {
                    throw EOFException("Stream closed")
                }
                timeout.onReceive {
                    throw TimeoutException("Timeout in receiving")
                }
                incomingFrames.onReceive {
                    currentFrame = it
                    readIdx = 0
                }
            }
        }
    }

    /***
     * implement write
     */
    fun getOutputStream(): OutputStream {
        return outStream
    }

    private val outStream = object : OutputStream() {

        override fun write(b: Int) {
            val ba = byteArrayOf(b.toByte())
            write(ba, 0, ba.size)
        }

        override fun write(b: ByteArray?) {
            if (b == null || b.size == 0)
                return
            write(b, 0, b.size )
        }

        override fun write(b: ByteArray?, off: Int, len: Int) {
            val frameSize = this@Stream.session.config.maxFrameSize
            if (b == null || b.size == 0)
                return

            var idx = 0
            while (idx < b.size) {
                val fsize = minOf(frameSize, b.size - idx)
                val frame = Frame(version, Command.PSH, this@Stream.id, b.sliceArray(idx until idx + fsize))
                idx += fsize
                runBlocking(CommonPool) { this@Stream.session.writeFrame(frame) }
            }
        }
    }

    suspend fun close() {
        val default = Channel<Any>(1)
        default.send(Any())
        select<Unit> {
            this@Stream.die.onReceiveOrNull {
                value -> if (value != null) this@Stream.die.close()
            }
            default.onReceive {
                // send fin
                this@Stream.die.close()
                val finFrame = Frame(version, Command.FIN, this@Stream.id, byteArrayOf())
                this@Stream.session.writeFrame(finFrame)
            }
        }
    }


    /***
     * session closes the stream
     */
    suspend fun sessionClose() {
        this.close()
    }

    suspend fun receive(frame: Frame) {
        incomingFrames.send(frame)
    }
}


