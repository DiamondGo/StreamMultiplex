package science.deepmemo.network.basic.smux

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert
import org.junit.Test
import java.util.*

class PipeStreamTest {
    @Test
    fun testPipeStream() {
        val pipeStream = PipeStream()
        val inputStream = pipeStream.getInput()
        val outputStream = pipeStream.getOutput()

        val data = ByteArray(1024 * 10) {(Random().nextInt() % 256).toByte()}

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

    @Test fun testRange() {
        (0 until 9).forEach { println(it) }
    }
}