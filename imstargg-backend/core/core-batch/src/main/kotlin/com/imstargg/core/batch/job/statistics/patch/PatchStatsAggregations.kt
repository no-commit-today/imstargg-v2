package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.BrawlerBattleRankPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerEnemyBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerPairBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerTrioBattleResultPatchStatsAggregator
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 다섯 유형의 정의. 테이블 이름과 컬럼 목록은 이름 표 그대로다.
 *
 * 키 컬럼의 순서는 집계기가 내는 keys 의 순서이고 유니크 `uk_{t}__1` 의 순서다(맨 앞의 brawlstars_patch_id 는 빼고 적는다).
 */
class PatchStatsAggregations(
    resultBattleReader: StatsBattleReader<StatsResultBattleEntity>,
    rankBattleReader: StatsBattleReader<StatsRankBattleEntity>,
) {
    val brawlerBattleResult: PatchStatsAggregation<StatsResultBattleEntity> =
        PatchStatsAggregation(
            statsType = StatsType.BRAWLER_BATTLE_RESULT,
            table =
                PatchStatsTable(
                    name = "brawler_battle_result_stats",
                    keyColumns = listOf("tier_range", "event_brawlstars_id", "brawler_brawlstars_id"),
                    countColumns = listOf("victory_count", "defeat_count", "draw_count", "total_count", "star_player_count"),
                ),
            reader = resultBattleReader,
            createAggregator = ::BrawlerBattleResultPatchStatsAggregator,
        )

    val brawlerBattleRank: PatchStatsAggregation<StatsRankBattleEntity> =
        PatchStatsAggregation(
            statsType = StatsType.BRAWLER_BATTLE_RANK,
            table =
                PatchStatsTable(
                    name = "brawler_battle_rank_stats",
                    keyColumns = listOf("tier_range", "event_brawlstars_id", "brawler_brawlstars_id"),
                    countColumns = (1..10).map { "rank${it}_count" } + "total_count",
                ),
            reader = rankBattleReader,
            createAggregator = ::BrawlerBattleRankPatchStatsAggregator,
        )

    val brawlerEnemyBattleResult: PatchStatsAggregation<StatsResultBattleEntity> =
        PatchStatsAggregation(
            statsType = StatsType.BRAWLER_ENEMY_BATTLE_RESULT,
            table =
                PatchStatsTable(
                    name = "brawler_enemy_battle_result_stats",
                    keyColumns = listOf("tier_range", "brawler_brawlstars_id", "enemy_brawler_brawlstars_id"),
                    countColumns = RESULT_COUNT_COLUMNS,
                ),
            reader = resultBattleReader,
            createAggregator = ::BrawlerEnemyBattleResultPatchStatsAggregator,
        )

    val brawlerPairBattleResult: PatchStatsAggregation<StatsResultBattleEntity> =
        PatchStatsAggregation(
            statsType = StatsType.BRAWLER_PAIR_BATTLE_RESULT,
            table =
                PatchStatsTable(
                    name = "brawler_pair_battle_result_stats",
                    keyColumns = listOf("tier_range", "brawler1_brawlstars_id", "brawler2_brawlstars_id"),
                    countColumns = RESULT_COUNT_COLUMNS,
                ),
            reader = resultBattleReader,
            createAggregator = ::BrawlerPairBattleResultPatchStatsAggregator,
        )

    val brawlerTrioBattleResult: PatchStatsAggregation<StatsResultBattleEntity> =
        PatchStatsAggregation(
            statsType = StatsType.BRAWLER_TRIO_BATTLE_RESULT,
            table =
                PatchStatsTable(
                    name = "brawler_trio_battle_result_stats",
                    keyColumns = listOf("tier_range", "brawler1_brawlstars_id", "brawler2_brawlstars_id", "brawler3_brawlstars_id"),
                    countColumns = RESULT_COUNT_COLUMNS,
                ),
            reader = resultBattleReader,
            createAggregator = ::BrawlerTrioBattleResultPatchStatsAggregator,
        )

    companion object {
        /** 상대·2인·3인 테이블의 값 컬럼. */
        private val RESULT_COUNT_COLUMNS = listOf("victory_count", "defeat_count", "draw_count", "total_count")
    }
}
