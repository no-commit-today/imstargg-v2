package com.imstargg.core.batch.job.statistics.pairbattleresult

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerPairBattleResultStatsBackupEntity
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsBackupJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class BrawlerPairBattleResultStatsWriterTest(
    private val sut: BrawlerPairBattleResultStatsWriter,
    private val repository: BrawlerPairBattleResultStatsBackupJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `버킷의 기존 행을 지우고 집계 결과를 넣되 다른 버킷은 건드리지 않는다`() {
        // given
        val bucketA = WeekRange(start = LocalDate.of(2026, 10, 26), end = LocalDate.of(2026, 10, 31))
        val bucketB = WeekRange(start = LocalDate.of(2026, 11, 1), end = LocalDate.of(2026, 11, 1))
        repository.saveAll(listOf(staleRow(bucketA), staleRow(bucketB)))
        val aggregator = BrawlerPairBattleResultStatsAggregator(bucketA.start, bucketA.end)
        aggregator.aggregate(
            resultBattle(
                teams =
                    listOf(
                        listOf(player(1), player(2), player(3)),
                        listOf(player(4, 1100), player(5, 1100), player(6, 1100)),
                    ),
            ),
        )

        // when
        val written = sut.write(bucketA, aggregator)

        // then
        val all = repository.findAll()
        val rowsA = all.filter { it.startBattleDate == bucketA.start && it.endBattleDate == bucketA.end }
        val rowsB = all.filter { it.startBattleDate == bucketB.start && it.endBattleDate == bucketB.end }
        assertThat(written).isEqualTo(6L)
        assertThat(rowsA).hasSize(6)
        assertThat(rowsA.map { it.brawler1BrawlStarsId to it.brawler2BrawlStarsId })
            .containsExactlyInAnyOrder(1L to 2L, 1L to 3L, 2L to 3L, 4L to 5L, 4L to 6L, 5L to 6L)
        assertThat(rowsA).noneMatch { it.brawler1BrawlStarsId == 10L && it.brawler2BrawlStarsId == 20L }
        assertThat(rowsB).hasSize(1)
        assertThat(rowsB.single().brawler1BrawlStarsId).isEqualTo(10L)
        assertThat(rowsB.single().brawler2BrawlStarsId).isEqualTo(20L)
    }

    private fun staleRow(weekRange: WeekRange): BrawlerPairBattleResultStatsBackupEntity =
        BrawlerPairBattleResultStatsBackupEntity(
            tierRange = StatsTierRange.TROPHY_1000_PLUS,
            startBattleDate = weekRange.start,
            endBattleDate = weekRange.end,
            brawler1BrawlStarsId = 10,
            brawler2BrawlStarsId = 20,
            resultCount = ResultCountEmbedded.create(victoryCount = 1, defeatCount = 0, drawCount = 0),
        )

    private fun resultBattle(teams: List<List<StatsBattleEntityTeamPlayer>>): StatsResultBattleEntity =
        StatsResultBattleEntity(
            battleKey = "battle-key",
            battleAt = OffsetDateTime.parse("2026-10-28T12:00:00+09:00").toInstant(),
            eventBrawlStarsId = 1,
            eventModeBrawlStarsId = 2,
            eventMap = "test-map",
            mode = "gemGrab",
            type = BattleType.RANKED.code,
            starPlayerBrawlStarsTag = null,
            playerBrawlStarsTag = teams.first().first().brawlStarsTag,
            teams = teams,
            result = BattleResult.VICTORY.code,
        )

    private fun player(
        index: Int,
        trophies: Int = 1000,
    ): StatsBattleEntityTeamPlayer =
        StatsBattleEntityTeamPlayer(
            brawlStarsTag = "#TAG$index",
            name = "Player$index",
            brawler =
                StatsBattleEntityTeamPlayerBrawler(
                    brawlStarsId = index.toLong(),
                    name = "Brawler$index",
                    power = 11,
                    trophies = trophies,
                ),
        )
}
