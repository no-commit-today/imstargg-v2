package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerBattleResultStatsReader>()
    private val writer = mockk<BrawlerBattleResultStatsWriter>()
    private val sut = BrawlerBattleResultStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 배틀을 집계해 writer 에 저장한다`() {
        // given
        val date = LocalDate.of(2026, 5, 1)
        val aggregatorSlot = slot<BrawlerBattleResultStatsAggregator>()
        every { reader.read(eq(date), any()) } returns 0L
        every { writer.write(eq(date), capture(aggregatorSlot)) } returns 0L

        // when
        sut.aggregate(date)

        // then
        verify(exactly = 1) { reader.read(eq(date), any()) }
        verify(exactly = 1) { writer.write(eq(date), any()) }
    }
}
