package science.deepmemo.network.basic.smux

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.junit.BeforeClass
import java.io.IOException
import kotlin.test.*

class SessionTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }

    @Test
    fun testStreamManage() = runBlocking<Unit>(CommonPool) {
        val config = Config.defaultConfig.copy(maxOpenStream = 3)
        val session = Session(config, ByteInputStream(), ByteOutputStream(), false)
        val s1 = session.openStream()!!
        val s2 = session.openStream()!!
        val s3 = session.openStream()!!

        assertTrue(s1 === session.getStreamById(s1.id))

        assertEquals(3, session.numStream())
        assertNull(session.openStream())

        session.streamClosed(s3)
        assertEquals(2, session.numStream())

        session.close()
        assertEquals(0, session.numStream())

        assertNotNull(session.openStream())
        assertNotNull(session.openStream())
        assertNotNull(session.openStream())
        assertNull(session.openStream())
    }
}