package com.imstargg.core.error

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.boot.logging.LogLevel

class ErrorLogger private constructor(
    private val log: KLogger,
) {
    companion object {
        fun of(log: KLogger): ErrorLogger = ErrorLogger(log)
    }

    fun log(ex: Throwable) {
        when (ex) {
            is CoreException -> logCoreException(ex)
            else -> logException(ex)
        }
    }

    fun log(
        ex: Exception,
        errorType: ErrorType,
    ) {
        val message = { "[${ex.javaClass.simpleName}] [$errorType]: ${errorType.message} >>> ${ex.message}" }
        when (errorType.logLevel) {
            LogLevel.ERROR -> log.error(ex, message)
            LogLevel.WARN -> log.warn(ex, message)
            else -> log.info(message)
        }
    }

    private fun logCoreException(ex: CoreException) {
        val message = { "[${ex.javaClass.simpleName}]: ${ex.message}" }
        when (ex.errorType.logLevel) {
            LogLevel.ERROR -> log.error(ex, message)
            LogLevel.WARN -> log.warn(ex, message)
            else -> log.info(message)
        }
    }

    private fun logException(ex: Throwable) {
        log.error(ex) { "[${ex.javaClass.simpleName}]: ${ex.message}" }
    }
}
