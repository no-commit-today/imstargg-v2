package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrawlerItemOwnershipStatisticsServiceTest(
    private val sut: BrawlerItemOwnershipStatisticsService,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `데이터가 없을 때 5종 ownership 메서드는 모두 빈 리스트를 반환한다`() {
        // given
        val brawlerId = BrawlStarsId(16_000_000L)
        val trophyRange = BrawlerTrophyRange.TROPHY_500_PLUS

        // when & then
        assertThat(sut.getGearOwnership(brawlerId, trophyRange)).isEmpty()
        assertThat(sut.getGadgetOwnership(brawlerId, trophyRange)).isEmpty()
        assertThat(sut.getStarPowerOwnership(brawlerId, trophyRange)).isEmpty()
        assertThat(sut.getHyperChargeOwnership(brawlerId, trophyRange)).isEmpty()
        assertThat(sut.getBuffyOwnership(brawlerId, trophyRange)).isEmpty()
    }
}
