package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant

/** 컨텍스트 테스트에서 직접 만들어 쓴다. */
class PatchStatsTestSupport(
    private val jdbcTemplate: JdbcTemplate,
    private val resultBattleJpaRepository: StatsResultBattleJpaRepository,
    private val rankBattleJpaRepository: StatsRankBattleJpaRepository,
) {
    /** 저장한 뒤 created_at 을 createdAt 으로 고친다. 같은 날짜의 전투는 픽스처의 battleKey 인자로 키를 다르게 준다. */
    fun saveResultBattle(
        battle: StatsResultBattleEntity,
        createdAt: Instant,
    ): StatsResultBattleEntity {
        val saved = resultBattleJpaRepository.saveAndFlush(battle)
        jdbcTemplate.update(
            "update stats_result_battle set created_at = ? where stats_result_battle_id = ?",
            Timestamp.from(createdAt),
            saved.id,
        )
        return saved
    }

    fun saveRankBattle(
        battle: StatsRankBattleEntity,
        createdAt: Instant,
    ): StatsRankBattleEntity {
        val saved = rankBattleJpaRepository.saveAndFlush(battle)
        jdbcTemplate.update(
            "update stats_rank_battle set created_at = ? where stats_rank_battle_id = ?",
            Timestamp.from(createdAt),
            saved.id,
        )
        return saved
    }

    /** 통계 테이블에서 한 패치의 줄을 키(문자열로 바꾼 값) → 값으로 읽는다. 컬럼 순서는 이름 표를 따른다. */
    fun statsOf(
        tableName: String,
        keyColumns: List<String>,
        countColumns: List<String>,
        brawlStarsPatchId: Long,
    ): Map<List<String>, List<Long>> {
        val sql =
            "select ${(keyColumns + countColumns).joinToString()} from $tableName where brawlstars_patch_id = ?"
        val rows =
            jdbcTemplate.query(
                sql,
                { rs, _ ->
                    val keys = keyColumns.indices.map { rs.getObject(it + 1).toString() }
                    val counts = countColumns.indices.map { rs.getLong(keyColumns.size + it + 1) }
                    keys to counts
                },
                brawlStarsPatchId,
            )
        return rows.toMap()
    }

    /** 전투 묶음을 새 집계기로 처음부터 집계한 값. statsOf 와 같은 모양이다. */
    fun <TBattle> expectedOf(
        aggregator: PatchStatsAggregator<TBattle>,
        battles: List<TBattle>,
    ): Map<List<String>, List<Long>> {
        battles.forEach { aggregator.aggregate(it) }
        return aggregator.toCounts().associate { count: PatchStatsCount ->
            count.keys.map { it.toString() } to count.counts
        }
    }
}
