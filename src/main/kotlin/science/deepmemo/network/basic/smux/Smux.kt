package science.deepmemo.network.basic.smux

import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

data class Config(
        val keepAliveInterval: Duration,
        val keepAliveTimeout: Duration,
        val receiveTimeout: Duration,
        val sendTimeout: Duration,
        val maxFrameSize: Int,
        val maxReceiveBuffer: Int,
        val maxOpenStream: Int,
        val maxStreamFrame: Int
) {
    companion object {
        val defaultConfig = Config(
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(300),
                Duration.ofSeconds(300),
                4096,
                4194304, // 4MB
                1048576, // 2**20, should be enough
                1024 // each stream will have this most frames in buffer
        )
    }
}

fun server(input: InputStream, output: OutputStream, config: Config = Config.defaultConfig): Session {
    return Session(config, input, output, false)
}

fun client(input: InputStream, output: OutputStream, config: Config = Config.defaultConfig): Session {
    return Session(config, input, output, true)
}
