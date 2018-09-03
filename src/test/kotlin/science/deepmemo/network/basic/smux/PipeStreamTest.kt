package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*

class PipeStreamTest {
    @Test fun testPipeStream() {
        val pipeStream = PipeStream()
        val inputStream = pipeStream.getInput()
        val outputStream = pipeStream.getOutput()

        val data = ByteArray(1024 * 10) { (Random().nextInt() % 256).toByte() }

        val inRun = launch {
            (0 until data.size).forEach {
                val byte = inputStream.read()
                Assert.assertEquals(byte.toByte(), data[it])
            }
        }
        val outRun = launch { outputStream.write(data) }

        runBlocking {
            inRun.join()
            outRun.join()
        }

    }

    @Test fun testByteIntConvert() {
        val pipeStream = PipeStream()
        val inputStream = pipeStream.getInput()
        val outputStream = pipeStream.getOutput()

        val myint = 123465
        val intbytes = ByteBuffer.allocate(4).putInt(myint).array()
        outputStream.write(intbytes)

        val buf = ByteArray(4)
        inputStream.read(buf)

        val myint2 = ByteBuffer.wrap(buf).getInt(0)
        Assert.assertEquals(myint, myint2)
    }
}