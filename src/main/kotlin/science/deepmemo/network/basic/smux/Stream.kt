package science.deepmemo.network.basic.smux


import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import java.io.InputStream
import java.io.OutputStream

class Stream(
        val id: Int,
        val session: Session
) {
    private val incomingFrames = Channel<Frame>(session.config.maxStreamFrame)
    private val incomingBytes = Channel<Byte>(session.config.maxReceiveBuffer)


    /***
     * implement read
     */
    fun getInputStream(): InputStream {
        return inStream
    }

    private val inStream = object : InputStream() {

        val incoming =launch {
            incomingFrames.consumeEach {
                it.data.forEach { incomingBytes.send(it) }
            }
        }

        override fun read(): Int {
            // add time out here
            return runBlocking { incomingBytes.receive().toInt() }
        }

        /*
        override fun read(b: ByteArray?): Int {
            return 0
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
            return 0
        }
        */
    }

    /***
     * implement write
     */
    fun getOutputStream(): OutputStream {
        return outStream
    }

    private val outStream = object : OutputStream() {
        override fun write(b: Int) {

        }

        /*
        override fun write(b: ByteArray?) {

        }

        override fun write(b: ByteArray?, off: Int, len: Int) {

        }
        */
    }

    fun close() {

    }

    /***
     * session closes the stream
     */
    fun sessionClose() {

    }
}


