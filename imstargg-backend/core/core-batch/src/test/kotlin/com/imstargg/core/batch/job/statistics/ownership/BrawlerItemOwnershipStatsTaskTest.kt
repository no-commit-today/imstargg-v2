package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsResult
import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
import com.imstargg.core.enums.BuffyType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatsTaskTest {
    private val reader = mockk<BrawlerItemOwnershipStatsReader>()
    private val writer = mockk<BrawlerItemOwnershipStatsWriter>()
    private val sut = BrawlerItemOwnershipStatsTask(reader, writer)

    @Test
    fun `리더가 전달한 스냅샷을 집계해 writer 에 저장한다`() {
        // given
        val snapshots =
            listOf(
                PlayerBrawlerOwnershipSnapshot(
                    playerBrawlerId = 1,
                    brawlerBrawlStarsId = 16000000,
                    trophies = 1000,
                    gearBrawlStarsIds = listOf(62000000),
                    gadgetBrawlStarsIds = listOf(23000000),
                    starPowerBrawlStarsIds = listOf(23000100),
                    hyperChargeBrawlStarsIds = listOf(23000200),
                    buffyTypes = listOf(BuffyType.GADGET),
                ),
            )
        val resultSlot = slot<BrawlerItemOwnershipStatsResult>()
        val consumeSlot = slot<(List<PlayerBrawlerOwnershipSnapshot>) -> Unit>()
        every { reader.read(capture(consumeSlot)) } answers {
            consumeSlot.captured.invoke(snapshots)
            snapshots.size.toLong()
        }
        every { writer.write(capture(resultSlot)) } just Runs

        // when
        sut.rebuild()

        // then
        val result = resultSlot.captured
        assertThat(result.gears).hasSize(2)
        assertThat(result.gears.map { it.gearBrawlStarsId }).containsOnly(62000000L)
        assertThat(result.gadgets).hasSize(2)
        assertThat(result.gadgets.map { it.gadgetBrawlStarsId }).containsOnly(23000000L)
        assertThat(result.starPowers).hasSize(2)
        assertThat(result.starPowers.map { it.starPowerBrawlStarsId }).containsOnly(23000100L)
        assertThat(result.hyperCharges).hasSize(2)
        assertThat(result.hyperCharges.map { it.hyperChargeBrawlStarsId }).containsOnly(23000200L)
        assertThat(result.buffies).hasSize(2)
        assertThat(result.buffies.map { it.buffyType }).containsOnly(BuffyType.GADGET)
        assertThat(result.totalEntityCount).isEqualTo(10)
        verify(exactly = 1) { writer.write(any()) }
    }
}
