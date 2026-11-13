package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.batch.domain.statistics.BrawlerBattleRankStatsAggregator
import com.imstargg.core.batch.domain.statistics.BrawlerBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.BrawlerTrioBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** 옛 집계기와 새 집계기에 같은 묶음을 넣어 같은 숫자가 나오는지 대조한다. 2단계에서 옛 집계기를 지울 때 함께 지운다. */
class PatchStatsAggregatorParityTest {
    @Test
    fun `옛 1인 승률 집계와 숫자가 같다`() {
        val old = BrawlerBattleResultStatsAggregator()
        val new = BrawlerBattleResultPatchStatsAggregator()
        resultBattles().forEach {
            old.aggregate(it)
            new.aggregate(it)
        }

        val expected =
            old
                .toEntities()
                .groupBy { Triple(it.tierRange.name, it.eventBrawlStarsId, it.brawlerBrawlStarsId) }
                .map { (key, entities) ->
                    PatchStatsCount(
                        keys = listOf(key.first, key.second, key.third),
                        counts = entities.map { it.resultCount }.sum().asList() + entities.sumOf { it.starPlayerCount },
                    )
                }

        assertThat(new.toCounts()).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `옛 1인 순위 집계와 숫자가 같다`() {
        val old = BrawlerBattleRankStatsAggregator()
        val new = BrawlerBattleRankPatchStatsAggregator()
        rankBattles().forEach {
            old.aggregate(it)
            new.aggregate(it)
        }

        val expected =
            old
                .toEntities()
                .filter { it.rankCounts.totalCount > 0 }
                .groupBy { Triple(it.tierRange.name, it.eventBrawlStarsId, it.brawlerBrawlStarsId) }
                .map { (key, entities) ->
                    val rankCounts = entities.map { it.rankCounts }.reduce(RankCountsEmbedded::plus)
                    PatchStatsCount(
                        keys = listOf(key.first, key.second, key.third),
                        counts = (1..10).map { rankCounts.countOf(it) } + rankCounts.totalCount,
                    )
                }

        assertThat(new.toCounts()).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `옛 상대 집계와 숫자가 같다`() {
        val old = BrawlerEnemyBattleResultStatsAggregator(START, END)
        val new = BrawlerEnemyBattleResultPatchStatsAggregator()
        resultBattles().forEach {
            old.aggregate(it)
            new.aggregate(it)
        }

        val expected =
            old.toEntities().map {
                PatchStatsCount(
                    keys = listOf(it.tierRange.name, it.brawlerBrawlStarsId, it.enemyBrawlerBrawlStarsId),
                    counts = it.resultCount.asList(),
                )
            }

        assertThat(new.toCounts()).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `옛 2인 집계와 숫자가 같다`() {
        val old = BrawlerPairBattleResultStatsAggregator(START, END)
        val new = BrawlerPairBattleResultPatchStatsAggregator()
        resultBattles().forEach {
            old.aggregate(it)
            new.aggregate(it)
        }

        val expected =
            old.toEntities().map {
                PatchStatsCount(
                    keys = listOf(it.tierRange.name, it.brawler1BrawlStarsId, it.brawler2BrawlStarsId),
                    counts = it.resultCount.asList(),
                )
            }

        assertThat(new.toCounts()).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `옛 3인 집계와 숫자가 같다`() {
        val old = BrawlerTrioBattleResultStatsAggregator(START, END)
        val new = BrawlerTrioBattleResultPatchStatsAggregator()
        resultBattles().forEach {
            old.aggregate(it)
            new.aggregate(it)
        }

        val expected =
            old.toEntities().map {
                PatchStatsCount(
                    keys = listOf(it.tierRange.name, it.brawler1BrawlStarsId, it.brawler2BrawlStarsId, it.brawler3BrawlStarsId),
                    counts = it.resultCount.asList(),
                )
            }

        assertThat(new.toCounts()).isNotEmpty().containsExactlyInAnyOrderElementsOf(expected)
    }

    /** 날짜가 다른 승·패·무, 티어 구간이 다른 트로피, 스타 플레이어, 봇 전투 하나를 섞은 묶음. */
    private fun resultBattles(): List<StatsResultBattleEntity> =
        listOf(
            threeVsThree(
                listOf(1000, 1100, 1200),
                listOf(1150, 1180, 1190),
                battleKey = "b1",
                battleAt = at("2026-11-01"),
                starPlayerBrawlStarsTag = "#TAG2",
            ),
            threeVsThree(
                listOf(2500, 1100, 2200),
                listOf(1150, 3000, 1190),
                result = BattleResult.DEFEAT.code,
                battleKey = "b2",
                battleAt = at("2026-11-03"),
                starPlayerBrawlStarsTag = "#TAG5",
            ),
            threeVsThree(
                listOf(1000, 1100, 1200),
                listOf(1150, 1180, 1190),
                result = BattleResult.DRAW.code,
                battleKey = "b3",
                battleAt = at("2026-11-09"),
            ),
            threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), battleKey = "bot", battleAt = at("2026-11-04")),
            threeVsThree(listOf(1000, 1100, 1200), listOf(1150, 1180, 1190), battleKey = "b4", battleAt = at("2026-11-09")),
        )

    /** 날짜가 다른 쇼다운 순위 여러 개와 봇 로비 하나. */
    private fun rankBattles(): List<StatsRankBattleEntity> {
        val solo = listOf(listOf(1500)) + List(9) { listOf(2100) }
        return listOf(
            showdown(StatsEventModeType.SOLO_SHOWDOWN, solo, battleRank = 1, battleKey = "r1", battleAt = at("2026-11-01")),
            showdown(StatsEventModeType.SOLO_SHOWDOWN, solo, battleRank = 4, battleKey = "r2", battleAt = at("2026-11-02")),
            showdown(StatsEventModeType.SOLO_SHOWDOWN, solo, battleRank = 10, battleKey = "r3", battleAt = at("2026-11-09")),
            showdown(StatsEventModeType.SOLO_SHOWDOWN, solo, battleRank = 0, battleKey = "r0", battleAt = at("2026-11-07")),
            showdown(
                StatsEventModeType.DUO_SHOWDOWN,
                listOf(listOf(1500, 2100), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)),
                battleRank = 2,
                battleKey = "r4",
                battleAt = at("2026-11-05"),
            ),
            showdown(
                StatsEventModeType.DUO_SHOWDOWN,
                listOf(listOf(1500, 2100), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)),
                battleKey = "bot-lobby",
                battleAt = at("2026-11-06"),
            ),
        )
    }

    private fun at(date: String): Instant = OffsetDateTime.parse("${LocalDate.parse(date)}T12:00:00+09:00").toInstant()

    private fun ResultCountEmbedded.asList(): List<Long> = listOf(victoryCount, defeatCount, drawCount, totalCount)

    private fun List<ResultCountEmbedded>.sum(): ResultCountEmbedded = reduce(ResultCountEmbedded::plus)

    companion object {
        private val START = LocalDate.parse("2026-10-26")
        private val END = LocalDate.parse("2026-11-01")
    }
}
