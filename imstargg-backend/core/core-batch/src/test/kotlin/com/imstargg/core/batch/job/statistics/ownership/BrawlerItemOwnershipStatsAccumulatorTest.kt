package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.BuffyType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatsAccumulatorTest {
    @Test
    fun `브롤러 보유 총수와 아이템 보유수를 trophy range 별로 집계한다`() {
        // given
        val sut = BrawlerItemOwnershipStatsAccumulator()

        // when
        sut.add(
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

        // then
        val result = sut.toResult()
        assertThat(result.gears).hasSize(2)
        assertThat(result.gears.map { it.trophyRange })
            .containsExactlyInAnyOrder(BrawlerTrophyRange.TROPHY_500_PLUS, BrawlerTrophyRange.TROPHY_1000_PLUS)
        assertThat(result.gears.map { it.ownerCount }).containsOnly(1)
        assertThat(result.gears.map { it.totalCount }).containsOnly(1)
        assertThat(result.gadgets).hasSize(2)
        assertThat(result.starPowers).hasSize(2)
        assertThat(result.hyperCharges).hasSize(2)
        assertThat(result.buffies).hasSize(2)
        assertThat(result.totalEntityCount).isEqualTo(10)
    }
}
