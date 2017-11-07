package science.deepmemo.network.basic.smux

import java.nio.ByteBuffer
import java.nio.ByteOrder

val version: Byte = 1

enum class Command(val value: Byte) {
    SYN(0),
    FIN(1),
    PSH(2),
    NOP(3);

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

val RawData.length: Int get() {
    val buffer = ByteBuffer.wrap(this.sliceArray((2..3))).order(ByteOrder.LITTLE_ENDIAN)
    return buffer.short.toInt()
}

val RawData.streamId: Int get() {
    val buffer = ByteBuffer.wrap(this.sliceArray((4..7))).order(ByteOrder.LITTLE_ENDIAN)
    return buffer.int
}

val RawData.data: ByteArray get() = this.sliceArray((8..(8 + this.length - 1)))


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
}

