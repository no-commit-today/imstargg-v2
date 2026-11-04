package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.StatsRate
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BattleResultStatisticsRepositoryTest(
    private val sut: BattleResultStatisticsRepository,
    private val resultStatsJpaRepository: BrawlerBattleResultStatsJpaRepository,
    private val pairStatsJpaRepository: BrawlerPairBattleResultStatsJpaRepository,
    private val enemyStatsJpaRepository: BrawlerEnemyBattleResultStatsJpaRepository,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    private val tierRange = StatsTierRange.TROPHY_1000_PLUS
    private val date = LocalDate.of(2026, 5, 1)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `findBrawlerResultStatistics - 게임 수 분모로 픽률 계산`() {
        val event = BrawlStarsId(100L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 1,
            ),
        )

        val result = sut.findBrawlerResultStatistics(event, tierRange, date, date)

        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().brawlerId).isEqualTo(BrawlStarsId(10L))
    }

    @Test
    fun `findBrawlerPairResultStatistics - 분모는 조합 테이블 전체 합 ÷ 3`() {
        // given - 조합 4개 각 총 30 → Σ 120 → 팀 40 → 픽률 30/40. X 가 낀 행(총 30)만 분모로 쓰면 0.75 가 나오지 않는다.
        listOf(10L to 20L, 30L to 40L, 50L to 60L, 70L to 80L).forEach { (brawler1, brawler2) ->
            savePair(brawler1, brawler2, victoryCount = 15, defeatCount = 15)
        }

        // when
        val result =
            sut.findBrawlerPairResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        val item = result.items.single()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(10L))
        assertThat(item.pairBrawlerId).isEqualTo(BrawlStarsId(20L))
        assertThat(item.battleCount).isEqualTo(30L)
        assertThat(item.pickRate).isEqualTo(0.75)
    }

    @Test
    fun `findBrawlerPairResultStatistics - X 가 brawler2 인 조합도 돌려준다`() {
        // given
        savePair(5L, 10L, victoryCount = 20, defeatCount = 10)

        // when
        val result =
            sut.findBrawlerPairResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        val item = result.items.single()
        assertThat(item.brawlerId).isEqualTo(BrawlStarsId(10L))
        assertThat(item.pairBrawlerId).isEqualTo(BrawlStarsId(5L))
    }

    @Test
    fun `findBrawlerEnemyResultStatistics - 분모는 X 행의 합 ÷ 3`() {
        // given - X(10) 행 3개 각 총 30 → Σ 90 → X 게임 30. (20 → 10) 행은 X 기준 행이 아니라 분모에 넣지 않는다.
        listOf(20L, 30L, 40L).forEach { enemy ->
            saveEnemy(10L, enemy, victoryCount = 15, defeatCount = 15)
        }
        saveEnemy(20L, 10L, victoryCount = 15, defeatCount = 15)

        // when
        val result =
            sut.findBrawlerEnemyResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        // then
        assertThat(result.items.map { it.enemyBrawlerId })
            .containsExactlyInAnyOrder(BrawlStarsId(20L), BrawlStarsId(30L), BrawlStarsId(40L))
        assertThat(result.items).allSatisfy { item ->
            assertThat(item.brawlerId).isEqualTo(BrawlStarsId(10L))
            assertThat(item.pickRate).isEqualTo(1.0)
        }
    }

    @Test
    fun `findBrawlerDailyResultStatistics - 날짜별 row 반환`() {
        val event = BrawlStarsId(103L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 2, defeatCount = 1, drawCount = 0),
                starPlayerCount = 1,
            ),
        )

        val result =
            sut.findBrawlerDailyResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().date).isEqualTo(date)
    }

    @Test
    fun `findBrawlerDailyResultStatistics - 같은 날짜의 여러 이벤트 row 를 병합한다`() {
        val event1 = BrawlStarsId(105L)
        val event2 = BrawlStarsId(106L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event1.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 1, drawCount = 0),
                starPlayerCount = 1,
            ),
        )
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event2.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 5, defeatCount = 2, drawCount = 1),
                starPlayerCount = 2,
            ),
        )

        val result =
            sut.findBrawlerDailyResultStatistics(
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        assertThat(result.items).hasSize(1)
        val item = result.items.first()
        assertThat(item.date).isEqualTo(date)
        assertThat(item.totalBattleCount).isEqualTo(12)
        assertThat(item.winRate).isEqualTo(StatsRate.winRate(victoryCount = 8, defeatCount = 3))
        assertThat(item.starPlayerRate).isEqualTo(0.25)
    }

    private fun savePair(
        brawler1: Long,
        brawler2: Long,
        victoryCount: Long,
        defeatCount: Long,
    ) {
        pairStatsJpaRepository.save(
            BrawlerPairBattleResultStatsEntity(
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawler1BrawlStarsId = brawler1,
                brawler2BrawlStarsId = brawler2,
                resultCount =
                    ResultCountEmbedded.create(victoryCount = victoryCount, defeatCount = defeatCount, drawCount = 0),
            ),
        )
    }

    private fun saveEnemy(
        brawler: Long,
        enemy: Long,
        victoryCount: Long,
        defeatCount: Long,
    ) {
        enemyStatsJpaRepository.save(
            BrawlerEnemyBattleResultStatsEntity(
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = brawler,
                enemyBrawlerBrawlStarsId = enemy,
                resultCount =
                    ResultCountEmbedded.create(victoryCount = victoryCount, defeatCount = defeatCount, drawCount = 0),
            ),
        )
    }
}
