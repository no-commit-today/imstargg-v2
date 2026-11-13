package com.imstargg.core.worker.schedule

import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.core.worker.application.stats.StatsEventModeSnapshot
import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StatsEventModeReloadSchedulerTest {
    @Test
    fun `다시 읽기에 실패해도 예외를 밖으로 내지 않고 이전 목록을 유지한다`() {
        // given
        val repository = mockk<StatsEventModeJpaRepository>()
        every { repository.findAll() } returns
            listOf(StatsEventModeEntity(20L, StatsEventModeType.RESULT_3VS3)) andThenThrows
            RuntimeException("db down")
        val snapshot = StatsEventModeSnapshot(repository)
        snapshot.reload()
        val sut = StatsEventModeReloadScheduler(snapshot)

        // when
        sut.reload()

        // then
        assertThat(snapshot.find(20L)).isEqualTo(StatsEventModeType.RESULT_3VS3)
    }
}
