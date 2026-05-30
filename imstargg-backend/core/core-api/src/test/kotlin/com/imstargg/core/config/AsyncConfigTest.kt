package com.imstargg.core.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.util.ReflectionTestUtils

class AsyncConfigTest {
    @Test
    fun `비동기 작업 executor는 종료 시 30초 동안 작업 완료를 기다린다`() {
        // given
        val sut = AsyncConfig()

        // when
        val executor = sut.asyncExecutor as ThreadPoolTaskExecutor

        // then
        assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown")).isEqualTo(true)
        assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis")).isEqualTo(30_000L)

        executor.destroy()
    }
}
