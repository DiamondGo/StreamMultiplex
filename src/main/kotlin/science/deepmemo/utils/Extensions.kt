package science.deepmemo.utils

import mu.KLogger
import mu.KotlinLogging

/*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LogManager.getLogger(this::class.java.name) }
}
*/

fun <R : Any> R.logger(): KLogger =
        KotlinLogging.logger {}

