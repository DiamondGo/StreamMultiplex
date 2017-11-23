package science.deepmemo.network.basic.smux

//import com.nhaarman.mockito_kotlin.*
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.junit.BeforeClass
import org.mockito.Mockito
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
*/

class StreamTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }

    /***
     * fix for mockito 2
     */
    private fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }
    private fun <T> uninitialized(): T = null as T

    @Test
    fun testInput() = runBlocking<Unit>(CommonPool) {
        val session = Session(Config.defaultConfig.copy(maxOpenStream = 4, maxFrameSize = 128, receiveTimeout = Duration.ofSeconds(2)), ByteInputStream(), ByteOutputStream(), true)
        val spySession = spy(session)

        val stream = spySession.openStream()
        assertNotNull(stream)

        val ins = stream!!.getInputStream()
        assertNotNull(ins)

        val ba = ByteArray(128) { it.toByte() }
        assertEquals(100.toByte(), ba[100])
        val frame = Frame(version, Command.PSH, stream.id, ba)

        stream.receive(frame)
        val baout1 = ByteArray(128) { 0 }
        val readcount1 = ins.read(baout1)
        assertEquals(128, readcount1)
        assertEquals(0.toByte(), baout1[0])
        assertEquals(77.toByte(), baout1[77])
        assertEquals(127.toByte(), baout1[127])

        stream.receive(frame)
        val baout2 = ByteArray(32) { 0 }
        val readcount2 = ins.read(baout2)
        assertEquals(32, readcount2)
        assertEquals(0.toByte(), baout2[0])
        assertEquals(31.toByte(), baout2[31])

        val b = ins.read()
        assertEquals(32, b)

        val baout3 = ByteArray(95) { 0 }
        val readcount3 = ins.read(baout3)
        assertEquals(95, readcount3)
        assertEquals(127.toByte(), baout3[94])

        val baout4 = ByteArray(16) { 0 }
        assertFailsWith<IOException> {
            ins.read(baout4)
        }


        val baout5 = ByteArray(16) { 0 }
        stream.close()
        assertEquals(-1, ins.read(baout5))
    }


    @Test
    fun testInputIncomplete() = runBlocking<Unit>(CommonPool) {
        val session = Session(Config.defaultConfig.copy(maxOpenStream = 4, maxFrameSize = 128, receiveTimeout = Duration.ofSeconds(2)), ByteInputStream(), ByteOutputStream(), true)
        val spySession = spy(session)

        val stream = spySession.openStream()
        assertNotNull(stream)

        val ins = stream!!.getInputStream()
        assertNotNull(ins)

        val ba = ByteArray(128) { it.toByte() }
        val frame = Frame(version, Command.PSH, stream.id, ba)
        stream.receive(frame)

        val baout1 = ByteArray(256) { 0 }
        launch {
            delay(500)
            stream.close()
        }
        val readcount1 = ins.read(baout1)
        assertEquals(128, readcount1)
        assertEquals(-1, ins.read(baout1))
        assertEquals(-1, ins.read())
    }

    @Test
    fun testOutput() = runBlocking<Unit>(CommonPool) {
        val session = Session(Config.defaultConfig.copy(maxOpenStream = 4, maxFrameSize = 128, receiveTimeout = Duration.ofSeconds(2)), ByteInputStream(), ByteOutputStream(), true)
        val spySession = spy(session)

        val frameBuf = mutableListOf<Frame>()

        doAnswer {
            //val frame: Frame = it.getArgument(0)!!
            val frame = it.arguments[0] as Frame
            frameBuf.add(frame)
        }.`when`(spySession).writeFrame(any())


    }


    @Test
    fun testInputStream() {
        val ba = ByteArray(16) {it.toByte()}
        val bis1 = ByteArrayInputStream(ba)

        val out = ByteArray(10)
        assertEquals(10, bis1.read(out))
        assertEquals(6, bis1.read(out))
        assertEquals(-1, bis1.read(out))

        val bis2 = ByteArrayInputStream(ba)
        assertEquals(10, bis2.read(out))

        bis2.close()
        assertEquals(6, bis2.read(out))

        val bis3 = ByteArrayInputStream(ba)
        val out2 = ByteArray(8)
        assertEquals(8, bis3.read(out2))
        assertEquals(8, bis3.read(out2))
        assertEquals(-1, bis3.read(out2))
        assertEquals(-1, bis3.read(out2))
    }

    /*
    @Test
    fun testInput() = runBlocking<Unit>(CommonPool) {

        val frameBuf = mutableListOf<Frame>()
        var sid = 100
        val mockSession = mock<Session> {
            on { isClosed() } doReturn false
            on { config } doReturn Config.defaultConfig.copy(maxFrame = 4)
        }
        whenever(mockSession.writeFrame(any())).thenAnswer {
            frameBuf.add(it.arguments[0] as Frame)
        }
        whenever(mockSession.openStream()).thenAnswer {
            Stream(sid++, mockSession)
        }

        assertEquals(false, mockSession.isClosed())

        val f = Frame(version, Command.PSH, 120, byteArrayOf(1,2))
        mockSession.writeFrame(f)
        mockSession.writeFrame(f)
        mockSession.writeFrame(f)
        assertEquals(3, frameBuf.size)

        val s1 = mockSession.openStream()
        val s2 = mockSession.openStream()
        assertEquals(101, s2!!.id)
    }
    */
}
