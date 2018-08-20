package science.deepmemo.network.basic.smux

import java.time.Duration

data class Config(
        val keepAliveInterval: Duration,
        val keepAliveTimeout: Duration,
        val receiveTimeout: Duration,
        val sendTimeout: Duration,
        val maxFrameSize: Int,
        val maxReceiveBuffer: Int,
        val maxOpenStream: Int,
        val maxFrameQueueSize: Int,
        val maxStreamFrame: Int
) {
    companion object {
        val defaultConfig = Config(
                keepAliveInterval = Duration.ofSeconds(10),
                keepAliveTimeout = Duration.ofSeconds(30),
                receiveTimeout = Duration.ofSeconds(300),
                sendTimeout = Duration.ofSeconds(300),
                maxFrameSize = 4096,
                maxReceiveBuffer = 4194304, // 4MB
                maxOpenStream = 1048576, // 2**20, should be enough
                maxFrameQueueSize = 1024,
                maxStreamFrame = 32 // each stream will have this most frames in buffer
        )
    }
}

