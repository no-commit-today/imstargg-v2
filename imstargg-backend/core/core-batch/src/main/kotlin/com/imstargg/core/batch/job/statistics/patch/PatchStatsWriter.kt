package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount
import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.enums.StatsType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.SqlParameterValue
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 패치 통계 줄과 체크포인트 줄을 한 트랜잭션으로 쓴다.
 *
 * ## 지키는 규칙
 * 한 패치의 통계는 "전투 날짜가 적용한 범위 안이고 id 가 체크포인트 이하"인 원본 전투를 집계한 값과 같다.
 * 그래서 통계 줄을 바꾸는 트랜잭션은 모두 체크포인트 줄을 함께 바꾼다.
 *
 * ## 쓰기 순서
 * 1. 첫 문장은 체크포인트 줄의 조건부 갱신(또는 삭제)이다. 수정 번호가 읽었을 때와 같을 때만 고치고,
 *    고쳐진 줄이 하나가 아니면 다른 실행이 먼저 쓴 것이므로 통계 줄을 건드리기 전에 실패한다.
 *    JPA 의 `@Version` 이나 더티 체킹을 쓰지 않는 것은 갱신이 커밋 때로 밀려 통계 줄보다 늦어지기 때문이다.
 * 2. 더하기는 키로 갱신하고 갱신된 줄이 없는 키만 넣는다. `on conflict`·`merge` 는 H2 와 PostgreSQL 에서
 *    같은 문장으로 돌지 않아 쓰지 않는다.
 * 3. 빼기는 뺄 수 있을 때만 갱신되게 조건을 건다. 갱신되지 않은 키가 있으면 통계와 원본이 어긋난 것이므로 실패한다.
 *    합이 0 이 된 줄은 지운다.
 *
 * 예외는 잡지 않는다. 트랜잭션이 되돌려지고 다음 실행이 체크포인트에서 잇는다.
 */
@Component
class PatchStatsWriter(
    private val jdbcTemplate: JdbcTemplate,
    txManager: PlatformTransactionManager,
) {
    private val transactionTemplate =
        TransactionTemplate(txManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    fun createCheckpoint(
        statsType: StatsType,
        brawlStarsPatchId: Long,
        dateRange: PatchDateRange,
    ) {
        transactionTemplate.executeWithoutResult {
            jdbcTemplate.update(
                INSERT_CHECKPOINT_SQL,
                statsType.name,
                brawlStarsPatchId,
                dateValue(dateRange.startDate),
                dateValue(dateRange.endDate),
            )
        }
    }

    fun add(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        checkpoint: StatsCheckpointUpdate,
        counts: List<PatchStatsCount>,
    ) {
        transactionTemplate.executeWithoutResult {
            updateCheckpoint(checkpoint)
            val missing = addToRows(table = table, brawlStarsPatchId = brawlStarsPatchId, counts = counts)
            insertRows(table = table, brawlStarsPatchId = brawlStarsPatchId, counts = missing)
        }
    }

    fun subtract(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        checkpoint: StatsCheckpointUpdate,
        counts: List<PatchStatsCount>,
    ) {
        transactionTemplate.executeWithoutResult {
            updateCheckpoint(checkpoint)
            subtractFromRows(table = table, brawlStarsPatchId = brawlStarsPatchId, counts = counts)
            deleteZeroRows(table, brawlStarsPatchId)
        }
    }

    /** 범위의 마지막 날을 뺀다. 체크포인트 줄을 지운다. */
    fun subtractLast(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        checkpointId: Long,
        version: Long,
        counts: List<PatchStatsCount>,
    ) {
        transactionTemplate.executeWithoutResult {
            val deleted = jdbcTemplate.update(DELETE_CHECKPOINT_WITH_VERSION_SQL, checkpointId, version)
            check(deleted == 1) {
                "체크포인트 줄의 수정 번호가 다르다. checkpointId=$checkpointId, version=$version, deleted=$deleted"
            }
            subtractFromRows(table = table, brawlStarsPatchId = brawlStarsPatchId, counts = counts)
            deleteZeroRows(table, brawlStarsPatchId)
        }
    }

    /**
     * 지워진 패치의 통계 줄과 체크포인트 줄을 지운다.
     *
     * 한 트랜잭션이 너무 커지지 않게 티어 구간마다 나눠 지운다. 중간에 죽어도 체크포인트 줄이 남아 있으므로
     * 다음 실행이 다시 지운다.
     */
    fun deletePatch(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        checkpointId: Long,
    ) {
        val sql = "delete from ${table.name} where brawlstars_patch_id = ? and tier_range = ?"
        StatsTierRange.entries.forEach { tierRange ->
            transactionTemplate.executeWithoutResult {
                jdbcTemplate.update(sql, brawlStarsPatchId, tierRange.name)
            }
        }
        transactionTemplate.executeWithoutResult {
            jdbcTemplate.update(DELETE_CHECKPOINT_SQL, checkpointId)
        }
    }

    private fun updateCheckpoint(checkpoint: StatsCheckpointUpdate) {
        val updated =
            jdbcTemplate.update(
                UPDATE_CHECKPOINT_SQL,
                checkpoint.lastReadBattleId,
                dateValue(checkpoint.appliedStartDate),
                dateValue(checkpoint.appliedEndDate),
                timestampValue(checkpoint.lastBattleAt),
                checkpoint.checkpointId,
                checkpoint.version,
            )
        check(updated == 1) {
            "체크포인트 줄의 수정 번호가 다르다. " +
                "checkpointId=${checkpoint.checkpointId}, version=${checkpoint.version}, updated=$updated"
        }
    }

    /** 키로 더하고, 갱신된 줄이 없는 키를 돌려준다. */
    private fun addToRows(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        counts: List<PatchStatsCount>,
    ): List<PatchStatsCount> {
        val sql =
            "update ${table.name} set " +
                table.countColumns.joinToString { "$it = $it + ?" } +
                ", updated_at = current_timestamp" +
                " where brawlstars_patch_id = ?" +
                table.keyColumns.joinToString("") { " and $it = ?" }
        return counts.chunked(BattleStatsConstants.BATCH_SIZE).flatMap { chunk ->
            val args = chunk.map { (it.counts + brawlStarsPatchId + it.keys).toTypedArray<Any>() }
            batchUpdateCounts(sql, args).zeroIndexes().map { chunk[it] }
        }
    }

    private fun insertRows(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        counts: List<PatchStatsCount>,
    ) {
        val columns = listOf("brawlstars_patch_id") + table.keyColumns + table.countColumns
        val sql =
            "insert into ${table.name} (${columns.joinToString()}, created_at, updated_at)" +
                " values (${columns.joinToString { "?" }}, current_timestamp, current_timestamp)"
        // insert 묶음의 줄별 결과는 reWriteBatchedInserts 에서 SUCCESS_NO_INFO 일 수 있어 보지 않는다.
        // 실패는 예외로 올라온다(유니크 위반 포함).
        counts.chunked(BattleStatsConstants.BATCH_SIZE).forEach { chunk ->
            jdbcTemplate.batchUpdate(sql, chunk.map { (listOf<Any>(brawlStarsPatchId) + it.keys + it.counts).toTypedArray() })
        }
    }

    private fun subtractFromRows(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
        counts: List<PatchStatsCount>,
    ) {
        val sql =
            "update ${table.name} set " +
                table.countColumns.joinToString { "$it = $it - ?" } +
                ", updated_at = current_timestamp" +
                " where brawlstars_patch_id = ?" +
                table.keyColumns.joinToString("") { " and $it = ?" } +
                table.countColumns.joinToString("") { " and $it >= ?" }
        counts.chunked(BattleStatsConstants.BATCH_SIZE).forEach { chunk ->
            val args = chunk.map { (it.counts + brawlStarsPatchId + it.keys + it.counts).toTypedArray<Any>() }
            batchUpdateCounts(sql, args).requireAllOne()
        }
    }

    private fun deleteZeroRows(
        table: PatchStatsTable,
        brawlStarsPatchId: Long,
    ) {
        jdbcTemplate.update("delete from ${table.name} where brawlstars_patch_id = ? and total_count = 0", brawlStarsPatchId)
    }

    private fun batchUpdateCounts(
        sql: String,
        args: List<Array<Any>>,
    ): BatchUpdateCounts {
        val result = jdbcTemplate.batchUpdate(sql, args)
        check(result.size == args.size) { "줄별 결과의 수가 보낸 줄 수와 다르다. sent=${args.size}, result=${result.size}" }
        return BatchUpdateCounts(result)
    }

    // PostgreSQL 드라이버는 타입 없는 null 과 Instant 를 받지 않으므로 SQL 타입을 밝혀 넘긴다.
    private fun dateValue(date: LocalDate?) = SqlParameterValue(Types.DATE, date)

    private fun timestampValue(instant: Instant?) = SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, instant?.atOffset(ZoneOffset.UTC))

    companion object {
        private const val INSERT_CHECKPOINT_SQL =
            "insert into stats_checkpoint (stats_type, brawlstars_patch_id, last_read_battle_id, applied_start_date," +
                " applied_end_date, last_battle_at, version, created_at, updated_at)" +
                " values (?, ?, 0, ?, ?, null, 0, current_timestamp, current_timestamp)"
        private const val UPDATE_CHECKPOINT_SQL =
            "update stats_checkpoint set last_read_battle_id = ?, applied_start_date = ?, applied_end_date = ?," +
                " last_battle_at = ?, version = version + 1, updated_at = current_timestamp" +
                " where stats_checkpoint_id = ? and version = ?"
        private const val DELETE_CHECKPOINT_WITH_VERSION_SQL =
            "delete from stats_checkpoint where stats_checkpoint_id = ? and version = ?"
        private const val DELETE_CHECKPOINT_SQL = "delete from stats_checkpoint where stats_checkpoint_id = ?"
    }
}
