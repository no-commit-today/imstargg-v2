package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.StatsCheckpointJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.LocalDate

class PatchStatsWriterTest(
    jdbcTemplate: JdbcTemplate,
    resultBattleJpaRepository: StatsResultBattleJpaRepository,
    rankBattleJpaRepository: StatsRankBattleJpaRepository,
    private val writer: PatchStatsWriter,
    private val checkpointJpaRepository: StatsCheckpointJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val support = PatchStatsTestSupport(jdbcTemplate, resultBattleJpaRepository, rankBattleJpaRepository)

    private val resultTable =
        PatchStatsTable(
            name = "brawler_battle_result_stats",
            keyColumns = listOf("tier_range", "event_brawlstars_id", "brawler_brawlstars_id"),
            countColumns = listOf("victory_count", "defeat_count", "draw_count", "total_count", "star_player_count"),
        )
    private val rankTable =
        PatchStatsTable(
            name = "brawler_battle_rank_stats",
            keyColumns = listOf("tier_range", "event_brawlstars_id", "brawler_brawlstars_id"),
            countColumns = (1..10).map { "rank${it}_count" } + "total_count",
        )

    private val patchId = 7L
    private val range = PatchDateRange(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10))
    private val keyA = listOf<Any>(StatsTierRange.TROPHY_1000_PLUS.name, 15000001L, 16000001L)
    private val keyB = listOf<Any>(StatsTierRange.TROPHY_2000_PLUS.name, 15000001L, 16000002L)
    private val keyC = listOf<Any>(StatsTierRange.TROPHY_3000_PLUS.name, 15000001L, 16000003L)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `없던 키는 넣고 있던 키는 더한다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(resultTable, patchId, update(checkpoint, 100L), listOf(count(keyA, 3, 1, 0, 4, 1)))
        val lastBattleAt = Instant.parse("2026-11-09T03:04:05Z")

        // when
        writer.add(
            resultTable,
            patchId,
            StatsCheckpointUpdate(
                checkpointId = checkpoint.id!!,
                version = 1L,
                lastReadBattleId = 200L,
                appliedStartDate = LocalDate.of(2026, 11, 5),
                appliedEndDate = LocalDate.of(2026, 11, 11),
                lastBattleAt = lastBattleAt,
            ),
            listOf(count(keyA, 1, 0, 0, 1, 0), count(keyB, 0, 2, 0, 2, 0)),
        )

        // then
        assertThat(resultStats()).isEqualTo(
            mapOf(
                keyA.asStrings() to listOf(4L, 1L, 0L, 5L, 1L),
                keyB.asStrings() to listOf(0L, 2L, 0L, 2L, 0L),
            ),
        )
        val saved = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        assertThat(saved.lastReadBattleId).isEqualTo(200L)
        assertThat(saved.appliedStartDate).isEqualTo(LocalDate.of(2026, 11, 5))
        assertThat(saved.appliedEndDate).isEqualTo(LocalDate.of(2026, 11, 11))
        assertThat(saved.lastBattleAt).isEqualTo(lastBattleAt)
        assertThat(saved.version).isEqualTo(2L)
    }

    @Test
    fun `1인 순위는 순위별 컬럼에 더한다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RANK)
        writer.add(rankTable, patchId, update(checkpoint, 10L), listOf(count(keyA, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3)))

        // when
        writer.add(
            rankTable,
            patchId,
            update(checkpoint, 20L, version = 1L),
            listOf(count(keyA, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 2)),
        )

        // then
        assertThat(rankStats()).isEqualTo(
            mapOf(keyA.asStrings() to listOf(1L, 1L, 2L, 0L, 0L, 0L, 0L, 0L, 0L, 1L, 5L)),
        )
        assertThat(findCheckpoint(StatsType.BRAWLER_BATTLE_RANK).version).isEqualTo(2L)
    }

    @Test
    fun `수정 번호가 다르면 실패하고 통계 줄은 그대로다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(resultTable, patchId, update(checkpoint, 100L), listOf(count(keyA, 3, 1, 0, 4, 1)))
        val statsBefore = resultStats()
        val checkpointBefore = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()

        // when & then
        assertThatThrownBy {
            writer.add(
                resultTable,
                patchId,
                update(checkpoint, 200L, version = 0L),
                listOf(count(keyA, 1, 0, 0, 1, 0), count(keyB, 0, 2, 0, 2, 0)),
            )
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(resultStats()).isEqualTo(statsBefore)
        assertThat(findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()).isEqualTo(checkpointBefore)
    }

    @Test
    fun `빼면 값이 줄고 합이 0 이 된 줄은 지워진다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(
            resultTable,
            patchId,
            update(checkpoint, 100L),
            listOf(count(keyA, 3, 1, 0, 4, 1), count(keyB, 0, 2, 0, 2, 0)),
        )

        // when
        writer.subtract(
            resultTable,
            patchId,
            update(checkpoint, 150L, version = 1L),
            listOf(count(keyA, 1, 0, 0, 1, 0), count(keyB, 0, 2, 0, 2, 0)),
        )

        // then
        assertThat(resultStats()).isEqualTo(mapOf(keyA.asStrings() to listOf(2L, 1L, 0L, 3L, 1L)))
        val saved = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        assertThat(saved.lastReadBattleId).isEqualTo(150L)
        assertThat(saved.version).isEqualTo(2L)
    }

    @Test
    fun `뺄 줄이 없으면 실패하고 아무것도 바뀌지 않는다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(resultTable, patchId, update(checkpoint, 100L), listOf(count(keyA, 3, 1, 0, 4, 1)))
        val statsBefore = resultStats()
        val checkpointBefore = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()

        // when & then
        assertThatThrownBy {
            writer.subtract(
                resultTable,
                patchId,
                update(checkpoint, 150L, version = 1L),
                listOf(count(keyA, 1, 0, 0, 1, 0), count(keyC, 1, 0, 0, 1, 0)),
            )
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(resultStats()).isEqualTo(statsBefore)
        assertThat(findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()).isEqualTo(checkpointBefore)
    }

    @Test
    fun `빼면 음수가 되는 값이 있으면 실패하고 아무것도 바뀌지 않는다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(
            resultTable,
            patchId,
            update(checkpoint, 100L),
            listOf(count(keyA, 3, 1, 0, 4, 1), count(keyB, 0, 2, 0, 2, 0)),
        )
        val statsBefore = resultStats()
        val checkpointBefore = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()

        // when & then
        assertThatThrownBy {
            writer.subtract(
                resultTable,
                patchId,
                update(checkpoint, 150L, version = 1L),
                listOf(count(keyA, 1, 0, 0, 1, 0), count(keyB, 1, 0, 0, 1, 0)),
            )
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(resultStats()).isEqualTo(statsBefore)
        assertThat(findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()).isEqualTo(checkpointBefore)
    }

    @Test
    fun `마지막 날을 빼면 체크포인트 줄도 지워진다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(
            resultTable,
            patchId,
            update(checkpoint, 100L),
            listOf(count(keyA, 3, 1, 0, 4, 1), count(keyB, 0, 2, 0, 2, 0)),
        )

        // when
        writer.subtractLast(
            resultTable,
            patchId,
            checkpoint.id!!,
            1L,
            listOf(count(keyA, 3, 1, 0, 4, 1), count(keyB, 0, 1, 0, 1, 0)),
        )

        // then
        assertThat(resultStats()).isEqualTo(mapOf(keyB.asStrings() to listOf(0L, 1L, 0L, 1L, 0L)))
        assertThat(checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT)).isEmpty()
    }

    @Test
    fun `마지막 날을 뺄 때 수정 번호가 다르면 실패한다`() {
        // given
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        writer.add(resultTable, patchId, update(checkpoint, 100L), listOf(count(keyA, 3, 1, 0, 4, 1)))
        val statsBefore = resultStats()
        val checkpointBefore = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()

        // when & then
        assertThatThrownBy {
            writer.subtractLast(resultTable, patchId, checkpoint.id!!, 0L, listOf(count(keyA, 3, 1, 0, 4, 1)))
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(resultStats()).isEqualTo(statsBefore)
        assertThat(findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT).snapshot()).isEqualTo(checkpointBefore)
    }

    @Test
    fun `같은 유형과 패치의 체크포인트 줄을 두 번 만들면 실패한다`() {
        // given
        writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId, range)

        // when & then
        assertThatThrownBy { writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId, range) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT)).hasSize(1)
    }

    @Test
    fun `체크포인트 줄을 처음 만들면 읽은 것이 없고 수정 번호가 0 이다`() {
        // when
        writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId, range)

        // then
        val saved = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        assertThat(saved.brawlStarsPatchId).isEqualTo(patchId)
        assertThat(saved.lastReadBattleId).isZero()
        assertThat(saved.appliedStartDate).isEqualTo(range.startDate)
        assertThat(saved.appliedEndDate).isEqualTo(range.endDate)
        assertThat(saved.lastBattleAt).isNull()
        assertThat(saved.version).isZero()
    }

    @Test
    fun `끝 날짜와 최근 전투 일시가 없어도 고친다`() {
        // given
        writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId, PatchDateRange(range.startDate, null))
        val checkpoint = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)

        // when
        writer.add(
            resultTable,
            patchId,
            StatsCheckpointUpdate(
                checkpointId = checkpoint.id!!,
                version = 0L,
                lastReadBattleId = 100L,
                appliedStartDate = range.startDate,
                appliedEndDate = null,
                lastBattleAt = null,
            ),
            emptyList(),
        )

        // then
        val saved = findCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        assertThat(saved.lastReadBattleId).isEqualTo(100L)
        assertThat(saved.appliedEndDate).isNull()
        assertThat(saved.lastBattleAt).isNull()
        assertThat(saved.version).isEqualTo(1L)
    }

    @Test
    fun `패치를 지우면 그 패치의 통계 줄과 체크포인트 줄만 사라진다`() {
        // given
        val otherPatchId = 8L
        writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId, range)
        writer.createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, otherPatchId, PatchDateRange(range.endDate!!, null))
        val checkpoints =
            checkpointJpaRepository
                .findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT)
                .associateBy { it.brawlStarsPatchId }
        val counts = listOf(count(keyA, 3, 1, 0, 4, 1), count(keyB, 0, 2, 0, 2, 0), count(keyC, 1, 0, 0, 1, 0))
        writer.add(resultTable, patchId, update(checkpoints.getValue(patchId), 100L), counts)
        writer.add(resultTable, otherPatchId, update(checkpoints.getValue(otherPatchId), 100L), counts)

        // when
        writer.deletePatch(resultTable, patchId, checkpoints.getValue(patchId).id!!)

        // then
        assertThat(resultStats()).isEmpty()
        assertThat(resultStats(otherPatchId)).hasSize(3)
        assertThat(checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT))
            .extracting<Long> { it.brawlStarsPatchId }
            .containsExactly(otherPatchId)
    }

    @Test
    fun `한 묶음보다 많은 키도 모두 쓴다`() {
        // given: 넣기가 두 묶음에 걸치고, 다음 더하기에서 없던 키가 둘째 묶음의 중간에 온다
        val checkpoint = createCheckpoint(StatsType.BRAWLER_BATTLE_RESULT)
        val keys = (1L..1002L).map { listOf<Any>(StatsTierRange.TROPHY_1000_PLUS.name, 15000001L, it) }
        val newKey = keys.last()
        writer.add(resultTable, patchId, update(checkpoint, 100L), keys.dropLast(1).map { count(it, 1, 0, 0, 1, 0) })

        // when
        writer.add(
            resultTable,
            patchId,
            update(checkpoint, 200L, version = 1L),
            keys.map { count(it, 1, 0, 0, 1, 0) },
        )

        // then
        val stats = resultStats()
        assertThat(stats).hasSize(1002)
        assertThat(stats.getValue(newKey.asStrings())).containsExactly(1L, 0L, 0L, 1L, 0L)
        assertThat(stats.filterKeys { it != newKey.asStrings() }.values)
            .hasSize(1001)
            .allSatisfy { assertThat(it).containsExactly(2L, 0L, 0L, 2L, 0L) }
    }

    private fun createCheckpoint(statsType: StatsType): StatsCheckpointEntity {
        writer.createCheckpoint(statsType, patchId, range)
        return findCheckpoint(statsType)
    }

    private fun findCheckpoint(statsType: StatsType): StatsCheckpointEntity = checkpointJpaRepository.findAllByStatsType(statsType).single()

    private fun update(
        checkpoint: StatsCheckpointEntity,
        lastReadBattleId: Long,
        version: Long = 0L,
    ) = StatsCheckpointUpdate(
        checkpointId = checkpoint.id!!,
        version = version,
        lastReadBattleId = lastReadBattleId,
        appliedStartDate = range.startDate,
        appliedEndDate = range.endDate,
        lastBattleAt = Instant.parse("2026-11-08T00:00:00Z"),
    )

    private fun count(
        keys: List<Any>,
        vararg counts: Long,
    ) = PatchStatsCount(keys, counts.toList())

    private fun List<Any>.asStrings(): List<String> = map { it.toString() }

    private fun resultStats(brawlStarsPatchId: Long = patchId) =
        support.statsOf(resultTable.name, resultTable.keyColumns, resultTable.countColumns, brawlStarsPatchId)

    private fun rankStats() = support.statsOf(rankTable.name, rankTable.keyColumns, rankTable.countColumns, patchId)

    private fun StatsCheckpointEntity.snapshot() = listOf(id, lastReadBattleId, appliedStartDate, appliedEndDate, lastBattleAt, version)
}
