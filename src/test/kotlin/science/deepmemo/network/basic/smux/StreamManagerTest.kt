package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class StreamManagerTest {
    lateinit var server: StreamServer
    lateinit var client: StreamClient
    val maxStream = 10

    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }

    @Before
    fun setup() {
        val pipeS2C = PipeStream()
        val inputS2C = pipeS2C.getInput()
        val outputS2C = pipeS2C.getOutput()

        val pipeC2S = PipeStream()
        val inputC2S = pipeC2S.getInput()
        val outputC2S = pipeC2S.getOutput()

        server = StreamServer(Config.defaultConfig.copy(maxOpenStream = maxStream), inputC2S, outputS2C)
        client = StreamClient(Config.defaultConfig.copy(maxOpenStream = maxStream), inputS2C, outputC2S)
    }

    @Test fun testOpenStream() {
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
        val loop = maxStream
        val jobServer = Thread {
            (0 until loop).forEach {
                val stream = server.accept()
                println("accept stream id ${stream.id}")
                Assert.assertEquals(it, stream.id)
            }
        }
        jobServer.start()


        val jobClient = Thread {
            (0 until loop).forEach {
                val stream = client.open()
                println("open stream id ${stream?.id}")
                Assert.assertEquals(it, stream?.id)
            }
        }
        jobClient.start()

        runBlocking {
            jobClient.join()
            jobServer.join()
        }
    }

    /*
    @Test
    fun testCloseStream() {
        setup()
        val clientStreams = (0..maxStream).map { client.open() }.filter { it != null }.map { it!! }.toMutableList()
        Assert.assertEquals(maxStream, clientStreams.size)

        Assert.assertNull(client.open())

        val serverStreams = mutableListOf<Stream>()
        launch(CommonPool) {
            while (true) {
                val stream = server.accept()
                serverStreams.add(stream)
            }
        }

        runBlocking { delay(1000) }
        Assert.assertEquals(maxStream, serverStreams.size)

        clientStreams[0].close()
        runBlocking { delay(1000) }
        //Assert.assertTrue(clientStreams[0].isClosed)
        //Assert.assertTrue(serverStreams[0].isClosed)

    }
    */

    @Test
    fun testNothing() {
        println("hehe")
    }
}