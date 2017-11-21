package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.CompletableDeferred
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

class Session (
        val config: Config,
        val inputStream: InputStream,
        val outputStream: OutputStream,
        val isClient: Boolean
) {

    /***
     * OpenStream is used to create a new stream.
     */
    suspend fun openStream(): Stream? {
        if (this.isClosed())
            throw IOException("Session is closed")

        val response = CompletableDeferred<Stream?>()
        streamManager.send(StreamMsg.Allocate(response))
        return response.await()
    }

    /***
     * AcceptStream is used to block until the next available stream
     * is ready to be accepted.
     */
    fun acceptStream(): Stream {
        return Stream(0, this)
    }

    suspend fun close() {
        streamManager.send(StreamMsg.CloseAll)
    }

    fun isClosed(): Boolean {
        return false
    }

    suspend fun numStream(): Int {
        val response = CompletableDeferred<Int>()
        streamManager.send(StreamMsg.GetCount(response))
        return response.await()
    }

    suspend fun getStreamById(streamId: Int): Stream? {
        val response = CompletableDeferred<Stream?>()
        streamManager.send(StreamMsg.GetById(streamId, response))
        return response.await()
    }
    /***
     * notify the session that a stream has closed.
     */
    suspend fun streamClosed(stream: Stream) {
        // remove from manager
        streamManager.send(StreamMsg.Free(stream))
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

    sealed class StreamMsg {
        object CloseAll : StreamMsg()
        class GetCount(val response: CompletableDeferred<Int>) : StreamMsg()
        class Allocate(val response: CompletableDeferred<Stream?>) : StreamMsg()
        class GetById(val streamId: Int, val response: CompletableDeferred<Stream?>) : StreamMsg()
        class Free(val stream: Stream) : StreamMsg()
    }

    val streamManager = streamManageActor()
    fun streamManageActor() = actor<StreamMsg> {
        var curStreamId = 0
        val streamMap = mutableMapOf<Int, Stream>()

        for (msg in channel) {
            when (msg) {
                is StreamMsg.GetCount -> msg.response.complete(streamMap.size)
                is StreamMsg.Allocate -> {
                    if (streamMap.size >= config.maxOpenStream) {
                        msg.response.complete(null)
                    } else {
                        while (streamMap.containsKey(curStreamId))
                            curStreamId = ((curStreamId.toLong() + 1L) % config.maxOpenStream.toLong()).toInt() // in case maxOpenStream is Int.MAX_VALUE
                        val stream = Stream(id = curStreamId, session = this@Session)
                        streamMap += curStreamId to stream
                        msg.response.complete(stream)
                    }
                }
                is StreamMsg.GetById -> msg.response.complete(streamMap[msg.streamId])
                is StreamMsg.Free -> streamMap.remove(msg.stream.id)
                is StreamMsg.CloseAll -> {
                    for ((_, stream) in streamMap)
                        stream.close()
                    streamMap.clear()
                }
            }
        }

    }

}


