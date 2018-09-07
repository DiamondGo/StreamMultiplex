package club.antigfw.network.basic.smux

import org.junit.Test
import org.junit.BeforeClass
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.*

class FrameTest {
    companion object {
        @JvmStatic
        @BeforeClass fun init() {
            //println("nada")
        }
    }

    @Test fun testRawData() {
        val bytes = byteArrayOf(version, Command.SYN.value,
                3, 0, // length
                1, 2, 3, 4, // streamId
                99, 100, 101) // data
        assertEquals(version, bytes.version)
        assertEquals(Command.SYN, bytes.command)
        assertEquals(3, bytes.dataSize)
        assertEquals(1 + (2 shl 8) + (3 shl 16) + (4 shl 24), bytes.streamId)
        val data = bytes.data
        assertEquals(3, data.size)
        assertEquals(99, data[0])
        assertEquals(101, data[2])

        val frame = Frame(bytes)
        assertEquals(version, frame.version)
        assertEquals(Command.SYN, frame.command)
        assertEquals(1 + (2 shl 8) + (3 shl 16) + (4 shl 24), frame.streamId)
        assertEquals(3, frame.data.size)
        assertEquals(100, frame.data[1])
    }

    @Test fun testReadWrite() {
        val output = ByteArrayOutputStream()
        val frame1 = Frame(version, Command.PSH, 0, byteArrayOf(1, 2, 3, 4))
        frame1.writeTo(output)
        val frame3 = Frame(version, Command.FIN, 99, byteArrayOf())
        frame3.writeTo(output)

        val ba = output.toByteArray()
        assertEquals(8 + frame1.data.size + 8, ba.size)

        val input = ByteArrayInputStream(ba)
        val frame2 = Frame.readFrom(input)

        assertEquals(frame1.version, frame2.version)
        assertEquals(frame1.command, frame2.command)
        assertEquals(frame1.streamId, frame2.streamId)
        assertEquals(frame1.data.size, frame2.data.size)
        (0 until frame1.data.size).forEach { assertEquals(frame1.data[it], frame2.data[it]) }

        val frame4 = Frame.readFrom(input)
        assertEquals(frame3.version, frame4.version)
        assertEquals(frame3.command, frame4.command)
        assertEquals(frame3.streamId, frame4.streamId)
    }

    @Test fun testSpecialFrame() {
        val finFrame = Frame.finFrame(100)
        assertEquals(version, finFrame.version)
        assertEquals(Command.FIN, finFrame.command)
        assertEquals(100, finFrame.streamId)
        assertEquals(0, finFrame.data.size)

        val closeFrame = Frame.closeFrame()
        assertEquals(version, closeFrame.version)
        assertEquals(Command.CLZ, closeFrame.command)
        assertEquals(0, closeFrame.streamId)
        assertEquals(0, closeFrame.data.size)
    }
}