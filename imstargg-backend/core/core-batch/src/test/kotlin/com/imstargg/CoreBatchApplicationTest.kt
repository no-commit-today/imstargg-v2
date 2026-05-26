package com.imstargg

import org.assertj.core.api.Assertions.assertThat
import org.springframework.core.env.Environment
import kotlin.test.Test

class CoreBatchApplicationTest(
    private val environment: Environment,
) : CoreBatchContextTest() {
    @Test
    fun contextLoads() {
    }

    @Test
    fun `graceful shutdown 설정이 적용된다`() {
        // given
        val serverShutdown = environment.getProperty("server.shutdown")
        val timeoutPerShutdownPhase = environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase")

        // when & then
        assertThat(serverShutdown).isEqualTo("graceful")
        assertThat(timeoutPerShutdownPhase).isEqualTo("30s")
    }
}
