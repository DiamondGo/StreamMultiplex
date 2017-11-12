package science.deepmemo.network.basic.smux

import org.junit.Test
import org.junit.BeforeClass
import kotlin.test.*

class FrameTest {
    companion object {
        @JvmStatic
        @BeforeClass fun init() {
            //println("nada")
        }
    }

    @Test
    fun testRawData() {
        val bytes = byteArrayOf(version, Command.SYN.value,
                3, 0, // length
                1, 2, 3, 4, // streamId
                99, 100, 101) // data
        assertEquals(version, bytes.version)
        assertEquals(Command.SYN, bytes.command)
        assertEquals(3, bytes.length)
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
}