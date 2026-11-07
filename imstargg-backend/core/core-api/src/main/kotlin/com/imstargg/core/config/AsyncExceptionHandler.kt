package com.imstargg.core.config

import com.imstargg.core.error.ErrorLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import java.lang.reflect.Method

private val log = KotlinLogging.logger {}

class AsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    private val errorLogger = ErrorLogger.of(log)

    override fun handleUncaughtException(
        ex: Throwable,
        method: Method,
        vararg params: Any?,
    ) {
        errorLogger.log(ex)
    }
}
