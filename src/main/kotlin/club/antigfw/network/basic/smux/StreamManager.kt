package club.antigfw.network.basic.smux

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import club.antigfw.utils.logger
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException

abstract class StreamManager(val config: Config, val input: InputStream, val output: OutputStream) {
    protected val log = logger()
    protected var curStreamId = 0
    protected val streamMap = mutableMapOf<Int, Stream>()
    protected val writeFrameQueue = Channel<Frame>(config.maxFrameQueueSize)
    //protected val synFrameQueue = Channel<Frame>(config.maxFrameQueueSize)
    protected val newStreamQueue = Channel<Stream>(config.maxOpenStream)
    protected var recvEnable = true

    protected val coroutineContex = if (config.dedicatedThread == 0) Dispatchers.Default else newFixedThreadPoolContext(2, "hehe")

    protected val recvLoop = GlobalScope.launch(coroutineContex, CoroutineStart.DEFAULT, {
        try {
            while (recvEnable) {
                //withTimeout(config.keepAliveInterval.toMillis())
                val frame = Frame.readFrom(input)
                when (frame.command) {
                    Command.SYN -> {
                        log.debug { "receive SYN frame id ${frame.streamId}" }
                        //synFrameQueue.send(frame) // only in server
                        val stream = synchronized(this@StreamManager.streamMap) {
                            if (frame.streamId in this@StreamManager.streamMap) {
                                log.error { "There are already a Stream with id ${frame.streamId}" }
                                // TODO("Send back message about conflict detected")
                                null
                            } else {
                                val stream = Stream(frame.streamId, this@StreamManager)
                                streamMap += frame.streamId to stream
                                stream
                            }
                        }
                        stream?.let { newStreamQueue.send(stream) }
                        log.debug { "Stream id ${frame.streamId} is accepted" }
                    }
                    Command.FIN -> {
                        log.debug { "receive FIN frame id ${frame.streamId}" }
                        val stream = synchronized(this@StreamManager.streamMap) {
                            this@StreamManager.streamMap[frame.streamId]
                        }
                        stream ?: log.error { "no stream with id ${frame.streamId} needs to be closed" }
                        stream?.stopReceiving()
                        stream?.let { checkReleaseStream(it) }
                    }
                    Command.CLZ -> TODO()
                    Command.PSH -> {
                        log.debug { "receive PSH frame id ${frame.streamId}, size ${frame.data.size}" }
                        val stream = synchronized(this@StreamManager.streamMap) {
                            this@StreamManager.streamMap[frame.streamId]
                        }
                        stream ?: log.error { "no stream with id ${frame.streamId} is currently receiving" }
                        stream?.receive(frame)
                    }
                    Command.NOP -> TODO()
                }
            }
        } catch (e: Exception) {
            when (e) {
                is EOFException -> log.info { "Remote stream closed" }
                is SocketException -> log.info { "Socket closed" }
                is IOException -> log.error { "Stream data corrupted" }
            }
            this@StreamManager.suddenDeath()
        }
    })

    protected fun suddenDeath() {
        // clear all resources without sending data
        recvEnable = false
        synchronized(streamMap) {
            streamMap.forEach {
                it.value.stopReceiving()
            }
            streamMap.clear()
        }
    }

    protected val sendLoop = GlobalScope.launch(coroutineContex, CoroutineStart.DEFAULT, {
        while (!writeFrameQueue.isClosedForReceive) {
            val frame = writeFrameQueue.receiveOrNull()
            frame?.let {
                it.writeTo(output)
            }
        }
    })

    fun writeFrame(frame: Frame) {
        runBlocking {
            writeFrameQueue.send(frame)
        }
    }

    fun checkReleaseStream(stream: Stream) {
        if (stream.isReceivingStopped && stream.isSendingStopped) {
            synchronized(streamMap) {
                streamMap.remove(stream.id)
            }
        }
    }
}

class StreamServer(config: Config, input: InputStream, output: OutputStream) : StreamManager(config, input, output) {
    fun accept(): Stream {
        val stream = runBlocking { newStreamQueue.receiveOrNull() } ?: throw ShutdownException("Peer closed, no more Stream")
        log.debug { "Stream id ${stream.id} is accepted" }
        return stream
    }
}

class StreamClient(config: Config, input: InputStream, output: OutputStream) : StreamManager(config, input, output) {


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
