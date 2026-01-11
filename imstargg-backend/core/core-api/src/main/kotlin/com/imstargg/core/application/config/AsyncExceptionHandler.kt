package com.imstargg.core.application.config

import com.example.template.core.error.ErrorLogger
import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import java.lang.reflect.Method

class AsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    private val errorLogger = ErrorLogger.of(LoggerFactory.getLogger(javaClass))

    override fun handleUncaughtException(
        ex: Throwable,
        method: Method,
        vararg params: Any?,
    ) {
        errorLogger.log(ex)
    }
}
