package science.deepmemo.network.basic.smux

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.InputStream
import java.io.OutputStream

class Stream(
        val id: Int,
        val session: Session,
        ) {
    /***
     * implement read
     */
    fun getInputStream(): InputStream {
        return ByteInputStream()
    }

    /***
     * implement write
     */
    fun getOutputStream(): OutputStream {
        return ByteOutputStream()
    }

    fun close() {

    }

    /***
     * session closes the stream
     */
    fun sessionClose() {

    }
}


