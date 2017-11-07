package science.deepmemo.network.basic.smux

import java.io.IOException
import kotlinx.coroutines.experimental.channels.*
import java.io.InputStream
import java.io.OutputStream

data class WriteRequest(
        val frame: Frame,
        val result: Channel<WriteResult>
)

data class WriteResult(
        val n: Int,
        val e: IOException?
)

class Session (val inputStream: InputStream,
               val outputStream: OutputStream
) {
    var nextStreamId: Int = 0

    /***
     * OpenStream is used to create a new stream.
     */
    fun openStream(): Stream {
        return Stream()
    }

    /***
     * AcceptStream is used to block until the next available stream
     * is ready to be accepted.
     */
    fun acceptStream(): Stream {
        return Stream()
    }

    fun close() {

    }

    fun isClosed(): Boolean {
        return false
    }

    fun numStreams(): Int {
        return 0
    }

    /***
     * notify the session that a stream has closed.
     */
    fun streamClosed(streamId: Int) {

    }

    /***
     * session read a frame from underlying connection
     * it's data is pointed to the input buffer
     */
    fun readFrame(): Frame {
        return Frame(version, Command.SYN, 0, byteArrayOf(1))
    }

    /***
     * recvLoop keeps on reading from underlying connection if tokens are available
     */
    private fun recvLoop() {

    }

    /***
     * send NOP
     */
    fun keepAlive() {

    }

    private fun sendLoop() {

    }

    /***
     * writeFrame writes the frame to the underlying connection
     * and returns the number of bytes written if successful
     */
    fun writeFrame(frame: Frame) {

    }
}


