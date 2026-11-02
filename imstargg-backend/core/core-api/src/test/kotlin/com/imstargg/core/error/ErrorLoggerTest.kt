package com.imstargg.core.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.oshai.kotlinlogging.slf4j.toKLogger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ErrorLoggerTest {
    @Test
    fun `info 레벨 CoreException 은 스택 트레이스를 남기지 않는다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log.toKLogger())
        val ex = CoreException(ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE, "tag=#ABC")

        // when
        sut.log(ex)

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.INFO)
        assertThat(appender.list.first().throwableProxy).isNull()
        assertThat(appender.list.first().formattedMessage)
            .isEqualTo(
                "[CoreException]: [BRAWLSTARS_API_UNDER_MAINTENANCE]:${ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE.message} >>> tag=#ABC",
            )
    }

    @Test
    fun `info 레벨 ErrorType 으로 남긴 예외는 스택 트레이스를 남기지 않는다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log.toKLogger())
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
        val sut = ErrorLogger.of(log.toKLogger())
        val ex = CoreException(ErrorType.DEFAULT_ERROR, "boom")

        // when
        sut.log(ex)

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.ERROR)
        assertThat(appender.list.first().throwableProxy).isNotNull()
    }

    @Test
    fun `처리하지 못한 예외는 ERROR 와 예외를 함께 남긴다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log.toKLogger())

        // when
        sut.log(RuntimeException("boom"))

        // then
        assertThat(appender.list).hasSize(1)
        assertThat(appender.list.first().level).isEqualTo(Level.ERROR)
        assertThat(appender.list.first().throwableProxy).isNotNull()
        assertThat(appender.list.first().formattedMessage).isEqualTo("[RuntimeException]: boom")
    }

    @Test
    fun `DEBUG 레벨 ErrorType 은 INFO 로 기록한다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log.toKLogger())

        // when
        sut.log(CoreException(ErrorType.NOT_FOUND))
        sut.log(IllegalArgumentException("bad"), ErrorType.INVALID_REQUEST)

        // then
        assertThat(appender.list).extracting<Level> { it.level }.containsExactly(Level.INFO, Level.INFO)
        assertThat(appender.list).allSatisfy { assertThat(it.throwableProxy).isNull() }
    }

    @Test
    fun `ErrorType 과 함께 남긴 예외는 타입·메시지·원인을 남긴다`() {
        // given
        val (log, appender) = loggerWithAppender()
        val sut = ErrorLogger.of(log.toKLogger())

        // when
        sut.log(IllegalStateException("maintenance"), ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)

        // then
        assertThat(appender.list.single().formattedMessage)
            .isEqualTo(
                "[IllegalStateException] [BRAWLSTARS_API_UNDER_MAINTENANCE]: " +
                    "${ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE.message} >>> maintenance",
            )
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
