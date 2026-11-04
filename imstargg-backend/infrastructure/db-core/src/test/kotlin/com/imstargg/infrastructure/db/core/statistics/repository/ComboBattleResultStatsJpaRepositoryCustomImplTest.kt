package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTrioBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ComboBattleResultStatsJpaRepositoryCustomImplTest(
    private val pairRepository: BrawlerPairBattleResultStatsJpaRepository,
    private val trioRepository: BrawlerTrioBattleResultStatsJpaRepository,
    private val enemyRepository: BrawlerEnemyBattleResultStatsJpaRepository,
) : DbCoreContextTest() {
    private val tierRange = StatsTierRange.TROPHY_1000_PLUS

    // 버킷 A: 2026-10-26 ~ 2026-10-31, 버킷 B: 2026-11-01 ~ 2026-11-01
    private val bucketAStart = LocalDate.of(2026, 10, 26)
    private val bucketAEnd = LocalDate.of(2026, 10, 31)
    private val bucketBStart = LocalDate.of(2026, 11, 1)
    private val bucketBEnd = LocalDate.of(2026, 11, 1)

    @Test
    fun `pair findAllByBrawler - X 가 brawler1 이든 brawler2 이든 찾고 버킷을 합산한다`() {
        // given
        savePair(10L, 20L, bucketAStart, bucketAEnd, victory = 1L)
        savePair(10L, 20L, bucketBStart, bucketBEnd, defeat = 1L)
        savePair(5L, 10L, bucketAStart, bucketAEnd, victory = 1L)

        // when
        val result = pairRepository.findAllByBrawler(10L, tierRange, bucketAStart, bucketBEnd)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            BrawlerPairResultProjection(10L, 20L, 1L, 1L, 0L),
            BrawlerPairResultProjection(5L, 10L, 1L, 0L, 0L),
        )
    }

    @Test
    fun `pair findAllByBrawler - 기간 밖 버킷은 더하지 않는다`() {
        // given
        savePair(10L, 20L, bucketAStart, bucketAEnd, victory = 1L)
        savePair(10L, 20L, bucketBStart, bucketBEnd, defeat = 1L)

        // when
        val result = pairRepository.findAllByBrawler(10L, tierRange, bucketBStart, bucketBEnd)

        // then
        assertThat(result).containsExactly(
            BrawlerPairResultProjection(10L, 20L, 0L, 1L, 0L),
        )
    }

    @Test
    fun `pair findAllHavingTotalCountAtLeast - 하한 미만 조합은 빠지고 sumTotalCount 는 모두 더한다`() {
        // given
        savePair(10L, 20L, bucketAStart, bucketAEnd, victory = 2L)
        savePair(10L, 20L, bucketBStart, bucketBEnd, defeat = 1L)
        savePair(10L, 30L, bucketAStart, bucketAEnd, victory = 1L)

        // when
        val result = pairRepository.findAllHavingTotalCountAtLeast(tierRange, bucketAStart, bucketBEnd, 2L)
        val totalCount = pairRepository.sumTotalCount(tierRange, bucketAStart, bucketBEnd)

        // then
        assertThat(result).containsExactly(
            BrawlerPairResultProjection(10L, 20L, 2L, 1L, 0L),
        )
        assertThat(totalCount).isEqualTo(4L)
    }

    @Test
    fun `trio findAllByBrawler - X 가 b1·b2·b3 어느 위치든 찾는다`() {
        // given
        saveTrio(10L, 20L, 30L, bucketAStart, bucketAEnd, victory = 1L)
        saveTrio(5L, 10L, 30L, bucketAStart, bucketAEnd, victory = 1L)
        saveTrio(1L, 2L, 10L, bucketAStart, bucketAEnd, victory = 1L)
        saveTrio(1L, 2L, 3L, bucketAStart, bucketAEnd, victory = 1L)

        // when
        val result = trioRepository.findAllByBrawler(10L, tierRange, bucketAStart, bucketBEnd)

        // then
        assertThat(result).containsExactlyInAnyOrder(
            BrawlerTrioResultProjection(10L, 20L, 30L, 1L, 0L, 0L),
            BrawlerTrioResultProjection(5L, 10L, 30L, 1L, 0L, 0L),
            BrawlerTrioResultProjection(1L, 2L, 10L, 1L, 0L, 0L),
        )
    }

    @Test
    fun `trio findAllHavingTotalCountAtLeast - 하한 미만 조합은 빠지고 sumTotalCount 는 모두 더한다`() {
        // given - (10,20,30) 은 두 버킷 합 3, (10,40,50) 은 1
        saveTrio(10L, 20L, 30L, bucketAStart, bucketAEnd, victory = 2L)
        saveTrio(10L, 20L, 30L, bucketBStart, bucketBEnd, defeat = 1L)
        saveTrio(10L, 40L, 50L, bucketAStart, bucketAEnd, victory = 1L)

        // when
        val result = trioRepository.findAllHavingTotalCountAtLeast(tierRange, bucketAStart, bucketBEnd, 2L)
        val totalCount = trioRepository.sumTotalCount(tierRange, bucketAStart, bucketBEnd)

        // then
        assertThat(result).containsExactly(
            BrawlerTrioResultProjection(10L, 20L, 30L, 2L, 1L, 0L),
        )
        assertThat(totalCount).isEqualTo(4L)
    }

    @Test
    fun `enemy findAllByBrawler - X 기준 행만 돌려준다`() {
        // given
        saveEnemy(10L, 20L, bucketAStart, bucketAEnd, victory = 1L)
        saveEnemy(20L, 10L, bucketAStart, bucketAEnd, defeat = 1L)

        // when
        val result = enemyRepository.findAllByBrawler(10L, tierRange, bucketAStart, bucketBEnd)

        // then
        assertThat(result).containsExactly(
            BrawlerEnemyResultProjection(10L, 20L, 1L, 0L, 0L),
        )
    }

    private fun savePair(
        brawler1: Long,
        brawler2: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        victory: Long = 0L,
        defeat: Long = 0L,
        draw: Long = 0L,
    ) = pairRepository.save(
        BrawlerPairBattleResultStatsEntity(
            tierRange = tierRange,
            startBattleDate = startDate,
            endBattleDate = endDate,
            brawler1BrawlStarsId = brawler1,
            brawler2BrawlStarsId = brawler2,
            resultCount = ResultCountEmbedded.create(victory, defeat, draw),
        ),
    )

    private fun saveTrio(
        brawler1: Long,
        brawler2: Long,
        brawler3: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        victory: Long = 0L,
        defeat: Long = 0L,
        draw: Long = 0L,
    ) = trioRepository.save(
        BrawlerTrioBattleResultStatsEntity(
            tierRange = tierRange,
            startBattleDate = startDate,
            endBattleDate = endDate,
            brawler1BrawlStarsId = brawler1,
            brawler2BrawlStarsId = brawler2,
            brawler3BrawlStarsId = brawler3,
            resultCount = ResultCountEmbedded.create(victory, defeat, draw),
        ),
    )

    private fun saveEnemy(
        brawler: Long,
        enemy: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        victory: Long = 0L,
        defeat: Long = 0L,
        draw: Long = 0L,
    ) = enemyRepository.save(
        BrawlerEnemyBattleResultStatsEntity(
            tierRange = tierRange,
            startBattleDate = startDate,
            endBattleDate = endDate,
            brawlerBrawlStarsId = brawler,
            enemyBrawlerBrawlStarsId = enemy,
            resultCount = ResultCountEmbedded.create(victory, defeat, draw),
        ),
    )
}
