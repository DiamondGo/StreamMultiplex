package club.antigfw.network.basic.smux

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.*
import java.util.concurrent.TimeoutException
import kotlin.math.min


class Stream(val id: Int, val manager: StreamManager) {
    private val incomingFrames = Channel<Frame>(manager.config.maxStreamFrame)

    fun forceClose() {
        stopReceiving()
        outStream.close()
    }
    fun stopReceiving() {
        incomingFrames.close()
    }
    val isReceivingStopped get() = incomingFrames.isClosedForSend

    suspend fun receive(frame: Frame) {
        incomingFrames.send(frame)
    }

    fun getInputStream(): InputStream = inStream

    private val inStream = object : InputStream() {
        private var currentFrame: Frame? = null
        private var readIdx = 0

        override fun read(): Int {
            val ba = ByteArray(1)
            val read = read(ba)
            return if (read <= 0) -1 else (ba[0].toInt() and 0xff)
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
                        runBlocking { getNextFrame() }
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
            return if (read > 0) read else -1
        }

        private suspend fun getNextFrame() {
            // TODO add timeout
            val frame = incomingFrames.receiveOrNull()
            if (frame != null) {
                currentFrame = frame
                readIdx = 0
            } else {
                throw EOFException("End of input stream")
            }
        }

        override fun close() {
        }

    }

    fun getOutputStream(): OutputStream = outStream

    private val outStream = object : OutputStream() {
        private var closed = false

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
            if (b == null || b.isEmpty() || len == 0 || off >= b.size || b.size - off < len) {
                return
            }

            val frameSize = this@Stream.manager.config.maxFrameSize
            generateSequence(0) { it + frameSize }
                    .takeWhile { it < len }
                    .forEach {
                        val frame = Frame(version, Command.PSH, this@Stream.id,
                                b.sliceArray(off + it until off + min(it + frameSize, len)))
                        manager.writeFrame(frame)
                    }
        }

        override fun close() {
            if (!closed) {
                closed = true
                manager.writeFrame(Frame.finFrame(this@Stream.id))
            }
            manager.checkReleaseStream(this@Stream)
        }

        val isClosed get() = closed
    }
    val isSendingStopped get() = outStream.isClosed
}

