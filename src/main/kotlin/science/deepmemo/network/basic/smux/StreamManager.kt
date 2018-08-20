package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import science.deepmemo.utils.logger
import java.io.InputStream
import java.io.OutputStream

abstract class StreamManager(val config: Config, val input: InputStream, val output: OutputStream) {
    protected val log = logger()
    protected var curStreamId = 0
    protected val streamMap = mutableMapOf<Int, Stream>()
    protected val readFrameQueue = Channel<Frame>(config.maxFrameQueueSize)
    protected val writeFrameQueue = Channel<Frame>(config.maxFrameQueueSize)
    protected val synFrameQueue = Channel<Frame>(config.maxFrameQueueSize)

    protected val recvLoop = launch(CommonPool) {
        while (!readFrameQueue.isClosedForSend) {
            //withTimeout(config.keepAliveInterval.toMillis()) {
            val frame = Frame.readFrom(input)
            when (frame.command) {
                Command.SYN -> synFrameQueue.send(frame) // only in server
                Command.FIN -> {
                    val stream = synchronized(this@StreamManager.streamMap) {
                        this@StreamManager.streamMap.remove(frame.streamId)
                    }
                    stream ?: log.error { "no stream with id ${frame.streamId} needs to be closed" }
                    stream?.close()
                }
                Command.CLZ -> TODO()
                Command.PSH -> {
                    val stream = synchronized(this@StreamManager.streamMap) {
                        this@StreamManager.streamMap[frame.streamId]
                    }
                    stream ?: log.error { "no stream with id ${frame.streamId} is currently receiving" }
                    stream?.receive(frame)
                }
                Command.NOP -> TODO()
            }
        }
    }

    protected val sendLoop = launch(CommonPool) {
        while (!writeFrameQueue.isClosedForReceive) {
            val frame = writeFrameQueue.receiveOrNull()
            frame?.let {
                log.info { "write ${frame.command.toString()} output" }
                it.writeTo(output)
            }
        }
    }

    fun writeFrame(frame: Frame):Unit {
        runBlocking {
            writeFrameQueue.send(frame)
        }
    }
}

class StreamServer(config: Config, input: InputStream, output: OutputStream) : StreamManager(config, input, output) {
    fun accept(): Stream {
        val frame = runBlocking { synFrameQueue.receiveOrNull() } ?: throw StreamClosedException("Stream closed")
        val stream = synchronized(this.streamMap) {
            if (this.streamMap.containsKey(frame.streamId)) {
                throw StreamIdOccupied("Stream id occupied")
            }
            val stream = Stream(frame.streamId, this)
            streamMap += frame.streamId to stream
            stream
        }
        log.debug { "Stream id $curStreamId is accepted" }
        return stream
    }
}

class StreamClient(config: Config, input: InputStream, output: OutputStream) : StreamManager(config, input, output){


    fun open(): Stream? {
        val stream = synchronized(this.streamMap) {
            if (streamMap.size >= config.maxOpenStream) {
                log.warn { "maximum Stream number ${config.maxOpenStream} has been reached" }
                null
            } else {
                while (streamMap.containsKey(curStreamId)) {
                    curStreamId = ((curStreamId.toLong() + 1L) % config.maxOpenStream.toLong()).toInt() // in case maxOpenStream is Int.MAX_VALUE
                }
                val stream = Stream(curStreamId, this)
                streamMap += curStreamId to stream
                log.debug { "Stream id $curStreamId is created" }
                stream
            }
        }
        if (stream != null) {
            val frame = Frame(version, Command.SYN, stream.id, byteArrayOf())
            writeFrame(frame)
        }
        return stream
    }
}
