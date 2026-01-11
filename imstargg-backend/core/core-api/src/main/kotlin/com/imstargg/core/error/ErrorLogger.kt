package com.example.template.core.error

import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.slf4j.Logger
import org.springframework.boot.logging.LogLevel
import kotlin.jvm.javaClass

class ErrorLogger private constructor(
    private val log: Logger,
) {
    companion object {
        private const val EXCEPTION_LOG_MESSAGE_TEMPLATE = "[{}]: {}"
        private const val EXCEPTION_LOG_MESSAGE_WITH_TYPE_TEMPLATE = "[{}] [{}]: {} >>> {}"

        fun of(log: Logger): ErrorLogger = ErrorLogger(log)
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
        when (errorType.logLevel) {
            LogLevel.ERROR ->
                log.error(
                    EXCEPTION_LOG_MESSAGE_WITH_TYPE_TEMPLATE,
                    ex.javaClass.simpleName,
                    errorType,
                    errorType.message,
                    ex.message,
                    ex,
                )
            LogLevel.WARN ->
                log.warn(
                    EXCEPTION_LOG_MESSAGE_WITH_TYPE_TEMPLATE,
                    ex.javaClass.simpleName,
                    errorType,
                    errorType.message,
                    ex.message,
                    ex,
                )
            else ->
                log.info(
                    EXCEPTION_LOG_MESSAGE_WITH_TYPE_TEMPLATE,
                    ex.javaClass.simpleName,
                    errorType,
                    errorType.message,
                    ex.message,
                    ex,
                )
        }
    }

    private fun logCoreException(ex: CoreException) {
        when (ex.errorType.logLevel) {
            LogLevel.ERROR -> log.error(EXCEPTION_LOG_MESSAGE_TEMPLATE, ex.javaClass.simpleName, ex.message, ex)
            LogLevel.WARN -> log.warn(EXCEPTION_LOG_MESSAGE_TEMPLATE, ex.javaClass.simpleName, ex.message, ex)
            else -> log.info(EXCEPTION_LOG_MESSAGE_TEMPLATE, ex.javaClass.simpleName, ex.message, ex)
        }
    }

    private fun logException(ex: Throwable) {
        log.error(EXCEPTION_LOG_MESSAGE_TEMPLATE, ex.javaClass.simpleName, ex.message, ex)
    }
}
