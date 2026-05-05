package com.example.template.core.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ErrorLoggerTest {
    @Test
    fun `info 레벨 CoreException 은 스택 트레이스를 남기지 않는다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log)
        val ex = CoreException(ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE, "tag=#ABC")

        // when
        sut.log(ex)

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.INFO)
        assertThat(appender.list.first().throwableProxy).isNull()
    }

    @Test
    fun `info 레벨 ErrorType 으로 남긴 예외는 스택 트레이스를 남기지 않는다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log)
        val ex = IllegalStateException("maintenance")

        // when
        sut.log(ex, ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.INFO)
        assertThat(appender.list.first().throwableProxy).isNull()
    }

    @Test
    fun `error 레벨 예외는 스택 트레이스를 남긴다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log)
        val ex = CoreException(ErrorType.DEFAULT_ERROR, "boom")

        // when
        sut.log(ex)

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.ERROR)
        assertThat(appender.list.first().throwableProxy).isNotNull()
    }

    private fun loggerWithAppender(): Pair<Logger, ListAppender<ILoggingEvent>> {
        val logger = LoggerFactory.getLogger("${javaClass.name}.${System.nanoTime()}") as Logger
        val appender = ListAppender<ILoggingEvent>()

        logger.level = Level.TRACE
        logger.isAdditive = false
        appender.context = logger.loggerContext
        appender.start()
        logger.addAppender(appender)

        return logger to appender
    }
}
