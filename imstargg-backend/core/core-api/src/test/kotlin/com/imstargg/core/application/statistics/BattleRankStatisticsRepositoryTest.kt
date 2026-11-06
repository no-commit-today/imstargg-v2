package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleRankStatsJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BattleRankStatisticsRepositoryTest(
    private val sut: BattleRankStatisticsRepository,
    private val soloRankStatsJpaRepository: BrawlerBattleRankStatsJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    private val tierRange = StatsTierRange.TROPHY_1000_PLUS
    private val date = LocalDate.of(2026, 5, 1)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `findBrawlerRankStatistics - solo rank entity 들을 브롤러별로 합산한 items 반환`() {
        val event = BrawlStarsId(200L)
        soloRankStatsJpaRepository.save(
            BrawlerBattleRankStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                rankCounts = RankCountsEmbedded.fromMap(mapOf(1 to 2L, 3 to 1L)),
            ),
        )

        val result = sut.findBrawlerRankStatistics(event, tierRange, date, date)

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(10L))
    }
}
