package science.deepmemo.network.basic.smux

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

val version: Byte = 1

enum class Command(val value: Byte) {
    SYN(0),
    FIN(1),
    PSH(2),
    NOP(3),
    CLZ(4);

    companion object {
        private val map = Command.values().associateBy(Command::value);
        fun fromInt(type: Byte) = map[type]
    }
}


typealias RawData = ByteArray

val RawData.version: Byte
    get() = this[0]

val RawData.command: Command
    get() = Command.fromInt(this[1])!!

val RawData.dataSize: Int get() {
    val buffer = ByteBuffer.wrap(this.sliceArray((2..3))).order(ByteOrder.LITTLE_ENDIAN)
    return buffer.short.toInt()
}

val RawData.streamId: Int get() {
    val buffer = ByteBuffer.wrap(this.sliceArray((4..7))).order(ByteOrder.LITTLE_ENDIAN)
    return buffer.int
}

val RawData.data: ByteArray get() = this.sliceArray((8..(8 + this.dataSize- 1)))


data class Frame(
        val version: Byte,
        val command: Command,
        val streamId: Int,
        val data: ByteArray
) {
    constructor(raw: RawData) : this(
            version = raw.version,
            command = raw.command,
            streamId = raw.streamId,
            data = raw.data)

    fun writeTo(os: OutputStream) {
        val buf = ByteBuffer.allocate(8)
        buf.order(ByteOrder.LITTLE_ENDIAN)
                .put(version)
                .put(command.value)
                .putShort(data.size.toShort())
                .putInt(streamId)
        os.write(buf.array())
        if (data.isNotEmpty())
            os.write(data)
    }

    companion object {
        fun readFrom(ins: InputStream): Frame {
            val header = ByteArray(8)
            if (ins.read(header) != 8)
                throw IOException("corrupt data")
            if (header.version != version)
                throw InputMismatchException("version mismatch, expect $version, actual ${header.version}")
            if (header.dataSize > 0) {
            }
            return when {
                header.dataSize > 0 -> {
                    val data = ByteArray(header.dataSize)
                    val dataSize = ins.read(data)
                    if (dataSize != header.dataSize)
                        throw InputMismatchException("dataSize not match, expect ${header.dataSize}, actual $dataSize")
                    Frame(header.version, header.command, header.streamId, data)
                }
                header.dataSize == 0 -> Frame(header.version, header.command, header.streamId, byteArrayOf())
                else -> throw IOException("invalid dataSize ${header.dataSize}")
            }
        }
    }
}

