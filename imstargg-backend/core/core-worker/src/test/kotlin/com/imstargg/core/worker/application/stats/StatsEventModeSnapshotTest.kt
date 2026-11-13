package com.imstargg.core.worker.application.stats

import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StatsEventModeSnapshotTest {
    private val repository = mockk<StatsEventModeJpaRepository>()
    private val sut = StatsEventModeSnapshot(repository)

    private val seeded =
        listOf(
            StatsEventModeEntity(20L, StatsEventModeType.RESULT_3VS3),
            StatsEventModeEntity(6L, StatsEventModeType.SOLO_SHOWDOWN),
        )

    @Test
    fun `읽은 행으로 모드 ID 의 유형을 찾는다`() {
        // given
        every { repository.findAll() } returns seeded

        // when
        sut.reload()

        // then
        assertThat(sut.find(20L)).isEqualTo(StatsEventModeType.RESULT_3VS3)
        assertThat(sut.find(6L)).isEqualTo(StatsEventModeType.SOLO_SHOWDOWN)
        assertThat(sut.find(25L)).isNull()
    }

    @Test
    fun `다시 읽기에 실패하면 예외를 던지고 이전 목록을 그대로 쓴다`() {
        // given
        every { repository.findAll() } returns seeded andThenThrows RuntimeException("db down")
        sut.reload()

        // when & then
        assertThrows<RuntimeException> { sut.reload() }
        assertThat(sut.find(20L)).isEqualTo(StatsEventModeType.RESULT_3VS3)
    }

    @Test
    fun `init 에서 읽기에 실패하면 예외가 밖으로 나간다`() {
        // given
        every { repository.findAll() } throws RuntimeException("db down")

        // when & then
        assertThrows<RuntimeException> { sut.init() }
    }

    @Test
    fun `빈 목록을 읽으면 아무 모드도 찾지 않는다`() {
        // given
        every { repository.findAll() } returns seeded andThen emptyList()
        sut.reload()

        // when
        sut.reload()

        // then
        assertThat(sut.find(20L)).isNull()
    }
}
