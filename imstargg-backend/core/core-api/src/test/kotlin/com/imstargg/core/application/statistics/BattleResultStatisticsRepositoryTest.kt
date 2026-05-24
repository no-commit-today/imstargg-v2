package com.imstargg.core.application.statistics

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
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
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
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

    private fun save3v3Event(eventId: Long): BrawlStarsId {
        // mode 의 brawlStarsId 는 event 와 별개 테이블 PK 라 충돌 위험 없음 — 단순 distinct 값.
        val mode =
            battleEventModeJpaRepository.save(
                BattleEventModeEntity(
                    brawlStarsId = 48000000L + eventId,
                    brawlStarsName = "gemGrab",
                    playerCounts = listOf(3, 3),
                ),
            )
        battleEventJpaRepository.save(BattleEventEntity(brawlStarsId = eventId, mode = mode, mapName = "X"))
        return BrawlStarsId(eventId)
    }

    @Test
    fun `findBrawlerResultStatistics - 3v3 이벤트의 게임 수 분모로 픽률 계산`() {
        val event = save3v3Event(eventId = 100L)
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
    fun `findBrawlerPairResultStatistics - 분모는 Result 슬롯합 기반 게임 수`() {
        val event = save3v3Event(eventId = 101L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 0,
            ),
        )
        pairStatsJpaRepository.save(
            BrawlerPairBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = 10L,
                pairBrawlerBrawlStarsId = 20L,
                resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 0, drawCount = 0),
            ),
        )

        val result =
            sut.findBrawlerPairResultStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                pairBrawlerId = null,
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().pairBrawlerId).isEqualTo(BrawlStarsId(20L))
    }

    @Test
    fun `findBrawlerPairResultStatistics - 다중 날짜에 걸친 분모는 일별 Result 슬롯합 환산의 합`() {
        val event = save3v3Event(eventId = 104L)
        val day1 = date
        val day2 = date.plusDays(1)
        // Result 슬롯: day1 6 슬롯(=1 게임) + day2 6 슬롯(=1 게임) → 분모 = 2
        listOf(day1, day2).forEach { d ->
            resultStatsJpaRepository.save(
                BrawlerBattleResultStatsEntity(
                    eventBrawlStarsId = event.value,
                    tierRange = tierRange,
                    battleDate = d,
                    brawlerBrawlStarsId = 10L,
                    resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                    starPlayerCount = 0,
                ),
            )
        }
        pairStatsJpaRepository.save(
            BrawlerPairBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = day1,
                endBattleDate = day2,
                brawlerBrawlStarsId = 10L,
                pairBrawlerBrawlStarsId = 20L,
                resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 1, drawCount = 0),
            ),
        )

        val result =
            sut.findBrawlerPairResultStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                pairBrawlerId = null,
                tierRange = tierRange,
                startDate = day1,
                endDate = day2,
            )

        assertThat(result.totalBattleCount).isEqualTo(2)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `findBrawlerEnemyResultStatistics - 분모는 Result 슬롯합 기반 게임 수`() {
        val event = save3v3Event(eventId = 102L)
        resultStatsJpaRepository.save(
            BrawlerBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                battleDate = date,
                brawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(victoryCount = 3, defeatCount = 3, drawCount = 0),
                starPlayerCount = 0,
            ),
        )
        enemyStatsJpaRepository.save(
            BrawlerEnemyBattleResultStatsEntity(
                eventBrawlStarsId = event.value,
                tierRange = tierRange,
                startBattleDate = date,
                endBattleDate = date,
                brawlerBrawlStarsId = 10L,
                enemyBrawlerBrawlStarsId = 30L,
                resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 0, drawCount = 0),
            ),
        )

        val result =
            sut.findBrawlerEnemyResultStatistics(
                eventId = event,
                brawlerId = BrawlStarsId(10L),
                tierRange = tierRange,
                startDate = date,
                endDate = date,
            )

        assertThat(result.totalBattleCount).isEqualTo(1)
        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().enemyBrawlerId).isEqualTo(BrawlStarsId(30L))
    }

    @Test
    fun `findBrawlerDailyResultStatistics - 날짜별 row 반환`() {
        val event = save3v3Event(eventId = 103L)
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
}
