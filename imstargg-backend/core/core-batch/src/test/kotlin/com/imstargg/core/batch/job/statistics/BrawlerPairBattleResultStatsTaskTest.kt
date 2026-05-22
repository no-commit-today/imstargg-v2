package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.WeekRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerPairBattleResultStatsTaskTest {
    private val reader = mockk<BrawlerPairBattleResultStatsReader>()
    private val writer = mockk<BrawlerPairBattleResultStatsWriter>()
    private val sut = BrawlerPairBattleResultStatsTask(reader, writer)

    @Test
    fun `주간 범위마다 이벤트를 조회해 이벤트별로 집계하고 저장한다`() {
        // given
        val weekRange1 =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))
        val weekRange2 =
            WeekRange(start = LocalDate.of(2026, 5, 5), end = LocalDate.of(2026, 5, 11))
        val weekRanges = listOf(weekRange1, weekRange2)

        every { reader.readDistinctEventIds(weekRange1) } returns listOf(10L, 11L)
        every { reader.readDistinctEventIds(weekRange2) } returns listOf(20L)
        every { reader.read(any(), any(), any()) } returns 0L
        every { writer.write(any(), any(), any()) } returns 0L

        // when
        sut.aggregate(weekRanges)

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange1) }
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange2) }
        verifyOrder {
            reader.read(weekRange1, 10L, any())
            writer.write(weekRange1, 10L, any())
            reader.read(weekRange1, 11L, any())
            writer.write(weekRange1, 11L, any())
            reader.read(weekRange2, 20L, any())
            writer.write(weekRange2, 20L, any())
        }
    }

    @Test
    fun `이벤트가 없으면 read 와 write 를 호출하지 않는다`() {
        // given
        val weekRange =
            WeekRange(start = LocalDate.of(2026, 4, 28), end = LocalDate.of(2026, 5, 4))

        every { reader.readDistinctEventIds(weekRange) } returns emptyList()

        // when
        sut.aggregate(listOf(weekRange))

        // then
        verify(exactly = 1) { reader.readDistinctEventIds(weekRange) }
        verify(exactly = 0) { reader.read(any(), any(), any()) }
        verify(exactly = 0) { writer.write(any(), any(), any()) }
    }
}
