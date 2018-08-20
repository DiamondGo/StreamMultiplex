package science.deepmemo.network.basic.smux

class StreamClosedException(message: String) : RuntimeException(message)

class StreamIdOccupied(message: String) : RuntimeException(message)
