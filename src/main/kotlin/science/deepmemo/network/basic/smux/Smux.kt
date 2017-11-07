package science.deepmemo.network.basic.smux

import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

data class Config(
        val keepAliveInterval: Duration,
        val keepAliveTimeout: Duration,
        val maxFrameSize: Int,
        val maxReceiveBuffer: Int
) {
    companion object {
        val defaultConfig = Config(
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                4096,
                4194304)
    }
}

fun server(input: InputStream, output: OutputStream, config: Config): Session {
    return Session(input, output)
}

fun client(input: InputStream, output: OutputStream, config: Config): Session {
    return Session(input, output)
}
