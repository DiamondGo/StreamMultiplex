package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import science.deepmemo.utils.logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class StreamManagerTest {
    protected val log = logger()
    private val maxStream = 10

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }

    @Before
    fun setup() {

    }

    @Test fun testOpenStream() {
        val pipeS2C = PipeStream()
        val inputS2C = pipeS2C.getInput()
        val outputS2C = pipeS2C.getOutput()

        val pipeC2S = PipeStream()
        val inputC2S = pipeC2S.getInput()
        val outputC2S = pipeC2S.getOutput()

        val server = StreamServer(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), inputC2S, outputS2C)
        val client = StreamClient(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), inputS2C, outputC2S)


        (0..maxStream*2).forEach {
            val stream = client.open()
            if (it < maxStream) {
                Assert.assertNotNull(stream)
            } else {
                Assert.assertNull(stream)
            }
        }
    }

    @Test fun testAcceptStream() {
        val pipeS2C = PipeStream()
        val inputS2C = pipeS2C.getInput()
        val outputS2C = pipeS2C.getOutput()

        val pipeC2S = PipeStream()
        val inputC2S = pipeC2S.getInput()
        val outputC2S = pipeC2S.getOutput()

        val server = StreamServer(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), inputC2S, outputS2C)
        val client = StreamClient(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), inputS2C, outputC2S)


        val loop = maxStream
        val jobServer = Thread {
            (0 until loop).forEach {
                val stream = server.accept()
                log.info { "accept stream id ${stream.id}" }
                Assert.assertEquals(it, stream.id)
            }
        }
        jobServer.start()


        val jobClient = Thread {
            (0 until loop).forEach {
                val stream = client.open()
                log.info { "open stream id ${stream?.id}" }
                Assert.assertEquals(it, stream?.id)
            }
        }
        jobClient.start()

        runBlocking {
            jobClient.join()
            jobServer.join()
        }
    }

    @Test fun testMultiStream() {
        val pipeS2C = PipeStream()
        val pipeC2S = PipeStream()

        testWithRealSteams(pipeC2S.getInput(), pipeC2S.getOutput(), pipeS2C.getInput(), pipeS2C.getOutput())
    }

    @Test fun testSocketStream() {
        val localhost = InetAddress.getLocalHost()
        val port = 9999
        val stop = Channel<Any>()

        val localReady = Channel<Any>()

        var remoteInput: InputStream = ByteArrayInputStream(byteArrayOf())
        var remoteOutput: OutputStream = ByteArrayOutputStream()
        val socketServer = Thread {
            val listener = ServerSocket(port, 0, localhost)
            val socket = listener.accept()
            remoteInput = socket.getInputStream()
            remoteOutput = socket.getOutputStream()

            runBlocking {
                stop.receive()
            }

            socket.close()
            listener.close()
        }
        socketServer.start()

        var localInput: InputStream = ByteArrayInputStream(byteArrayOf())
        var localOutput: OutputStream = ByteArrayOutputStream()
        val socketClient = Thread {
            runBlocking {
                //remoteReady.receiveOrNull()
                delay(1000)
            }

            val s = Socket(localhost, port)
            localInput = s.getInputStream()
            localOutput = s.getOutputStream()

            runBlocking {
                localReady.send(Any())
                stop.receive()
            }

            s.close()
        }
        socketClient.start()

        runBlocking {
            localReady.receiveOrNull()
        }

        testWithRealSteams(localInput, remoteOutput, remoteInput, localOutput)

        runBlocking {
            stop.send(Any())
            stop.send(Any())
        }
        socketServer.join()
        socketClient.join()
    }


    fun testWithRealSteams(localInput: InputStream, remoteOutput: OutputStream,
                           remoteInput: InputStream, localOutput: OutputStream) {
        val server = StreamServer(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), localInput, localOutput)
        val client = StreamClient(Config.defaultConfig.copy(maxOpenStream = maxStream, dedicatedThread = 2, maxFrameSize = 64), remoteInput, remoteOutput)

        val rand = Random()
        val minRand = 200
        val maxRand = 8000
        val threadSet = mutableSetOf<Thread>()

        val threadCount = maxStream
        (0 until threadCount).forEach {
            val thread = Thread {
                val stream = client.open()
                val bytes = ByteArray(rand.nextInt(maxRand - minRand) + minRand) {rand.nextInt(256).toByte()}
                val input = stream!!.getInputStream()
                val output = stream!!.getOutputStream()

                output.write(bytes)
                output.close()


                (0 until bytes.size).forEach {
                    Assert.assertEquals(bytes[it].toInt() and 0xFF, input.read())
                }
                Assert.assertEquals(-1, input.read())

            }
            thread.start()
            threadSet.add(thread)
        }

        (0 until threadCount).forEach {
            val stream = server.accept()
            val thread = Thread {
                val buf = ByteArray(32)
                val input = stream!!.getInputStream()
                val output = stream!!.getOutputStream()

                var readcnt = 0
                do {
                    readcnt = input.read(buf)
                    if (readcnt > 0) {
                        output.write(buf, 0, readcnt)
                    }
                } while (readcnt > 0)
                output.close()
            }
            thread.start()
            threadSet.add(thread)
        }

        log.info { "waiting for ${threadSet.size} threads to finish" }
        threadSet.forEach {
            it.join()
        }

    }

}