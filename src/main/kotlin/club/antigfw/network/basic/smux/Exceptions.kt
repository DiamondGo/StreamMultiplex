package club.antigfw.network.basic.smux

class StreamClosedException(message: String) : RuntimeException(message)

class ShutdownException(message: String) : RuntimeException(message)

class StreamIdOccupied(message: String) : RuntimeException(message)
