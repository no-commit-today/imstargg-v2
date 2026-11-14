package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.patch.BrawlerBattleRankPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerEnemyBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerPairBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.BrawlerTrioBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.StatsCheckpointJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import io.mockk.mockk
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class PatchStatsTaskTest(
    entityManagerFactory: EntityManagerFactory,
    private val jdbcTemplate: JdbcTemplate,
    resultBattleJpaRepository: StatsResultBattleJpaRepository,
    rankBattleJpaRepository: StatsRankBattleJpaRepository,
    private val patchJpaRepository: BrawlStarsPatchJpaRepository,
    private val checkpointJpaRepository: StatsCheckpointJpaRepository,
    private val writer: PatchStatsWriter,
    private val rangeAligner: PatchStatsRangeAligner,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val support = PatchStatsTestSupport(jdbcTemplate, resultBattleJpaRepository, rankBattleJpaRepository)
    private val resultReader = StatsResultBattleReader(entityManagerFactory, pageSize = 2)
    private val rankReader = StatsRankBattleReader(entityManagerFactory, pageSize = 2)
    private val aggregations = PatchStatsAggregations(resultReader, rankReader)

    private val now: Instant = OffsetDateTime.parse("2026-11-20T10:00:00+09:00").toInstant()
    private val defaultCreatedAt: Instant = now.minus(Duration.ofHours(1))
    private val range1103 = PatchDateRange(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10))
    private val range1111 = PatchDateRange(LocalDate.of(2026, 11, 12), null)

    // 이름 표를 그대로 옮겨 적는다. 정의(PatchStatsAggregations)와 따로 적어 정의가 틀리면 어긋나게 한다.
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
    private val enemyTable =
        PatchStatsTable(
            name = "brawler_enemy_battle_result_stats",
            keyColumns = listOf("tier_range", "brawler_brawlstars_id", "enemy_brawler_brawlstars_id"),
            countColumns = listOf("victory_count", "defeat_count", "draw_count", "total_count"),
        )
    private val pairTable =
        PatchStatsTable(
            name = "brawler_pair_battle_result_stats",
            keyColumns = listOf("tier_range", "brawler1_brawlstars_id", "brawler2_brawlstars_id"),
            countColumns = listOf("victory_count", "defeat_count", "draw_count", "total_count"),
        )
    private val trioTable =
        PatchStatsTable(
            name = "brawler_trio_battle_result_stats",
            keyColumns = listOf("tier_range", "brawler1_brawlstars_id", "brawler2_brawlstars_id", "brawler3_brawlstars_id"),
            countColumns = listOf("victory_count", "defeat_count", "draw_count", "total_count"),
        )

    private val savedResults = mutableListOf<StatsResultBattleEntity>()
    private val savedRanks = mutableListOf<StatsRankBattleEntity>()
    private var battleKeySeq = 0

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `체크포인트 뒤의 전투만 더한다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 8))
        saveResult(LocalDate.of(2026, 11, 13))
        val firstCap = saveResult(LocalDate.of(2026, 11, 15))
        task().execute(aggregations.brawlerBattleResult)
        saveResult(LocalDate.of(2026, 11, 9))
        val secondCap = saveResult(LocalDate.of(2026, 11, 16))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(firstCap.id).isLessThan(secondCap.id)
        assertThat(resultCheckpointOf(patch1103).lastReadBattleId).isEqualTo(secondCap.id)
        assertThat(resultCheckpointOf(patch1111).lastReadBattleId).isEqualTo(secondCap.id)
        assertThat(assertResultStats(patch1103, range1103)).isNotEmpty
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
    }

    @Test
    fun `두 번 돌려도 숫자가 같다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        saveResult(LocalDate.of(2026, 11, 14))
        task().execute(aggregations.brawlerBattleResult)
        val first1103 = resultStatsOf(patch1103)
        val first1111 = resultStatsOf(patch1111)

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(first1103).isNotEmpty
        assertThat(first1111).isNotEmpty
        assertThat(resultStatsOf(patch1103)).isEqualTo(first1103)
        assertThat(resultStatsOf(patch1111)).isEqualTo(first1111)
        assertResultStats(patch1103, range1103)
        assertResultStats(patch1111, range1111)
    }

    @Test
    fun `저장 단위에서 끊겨도 다음 실행이 이어서 한다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        val battles = (0 until 10).map { saveResult(LocalDate.of(2026, 11, 12 + it % 5)) }
        val failingReader = FailingOnPageBattleReader(resultReader, failingPage = 3)
        val failingAggregation =
            aggregations.brawlerBattleResult.let {
                PatchStatsAggregation(it.statsType, it.jobName, it.table, failingReader, it.createAggregator)
            }
        assertThatThrownBy { task().execute(failingAggregation) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("3 번째 쪽")
        val stopped = resultCheckpointOf(patch1111)
        assertThat(stopped.lastReadBattleId).isEqualTo(battles[3].id)
        assertThat(stopped.version).isEqualTo(1L)
        assertResultStats(patch1111, range1111)

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        val resumed = resultCheckpointOf(patch1111)
        assertThat(resumed.lastReadBattleId).isEqualTo(battles.last().id)
        assertThat(resumed.version).isEqualTo(3L)
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
        assertThat(resultCheckpointOrNull(patch1103)).isNull()
    }

    @Test
    fun `새 전투가 없어도 체크포인트가 상한 id 로 오른다`() {
        // given
        val (patch1103, _) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        val before = resultStatsOf(patch1103)
        val outOfRange = saveResult(LocalDate.of(2026, 11, 14))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        val checkpoint = resultCheckpointOf(patch1103)
        assertThat(checkpoint.lastReadBattleId).isEqualTo(outOfRange.id)
        assertThat(checkpoint.version).isEqualTo(2L)
        assertThat(before).isNotEmpty
        assertThat(resultStatsOf(patch1103)).isEqualTo(before)
        assertResultStats(patch1103, range1103)
    }

    @Test
    fun `패치 당일의 전투는 어느 패치에도 들어가지 않는다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        val patchDay = saveResult(LocalDate.of(2026, 11, 11), trophies = 2500)
        saveResult(LocalDate.of(2026, 11, 13))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOf(patch1103).lastReadBattleId).isGreaterThan(patchDay.id)
        assertThat(resultCheckpointOf(patch1111).lastReadBattleId).isGreaterThan(patchDay.id)
        assertThat(assertResultStats(patch1103, range1103)).isNotEmpty
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
    }

    @Test
    fun `늦게 들어온 전투는 지난 패치에 들어간다`() {
        // given
        val (patch1103, _) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        val late = saveResult(LocalDate.of(2026, 11, 10), trophies = 2500)
        saveResult(LocalDate.of(2026, 11, 14))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOf(patch1103).lastReadBattleId).isGreaterThan(late.id)
        assertThat(assertResultStats(patch1103, range1103)).isNotEmpty
    }

    @Test
    fun `가장 이른 패치보다 앞선 날짜의 전투는 집계하지 않는다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 2), trophies = 2500)
        saveResult(LocalDate.of(2026, 11, 5))
        saveResult(LocalDate.of(2026, 11, 13))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(assertResultStats(patch1103, range1103)).isNotEmpty
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
    }

    @Test
    fun `범위의 첫 날이 원본에 없으면 줄을 만들지 않는다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 6))
        saveResult(LocalDate.of(2026, 11, 13))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOrNull(patch1103)).isNull()
        assertThat(resultStatsOf(patch1103)).isEmpty()
        assertThat(resultCheckpointOf(patch1111).appliedStartDate).isEqualTo(range1111.startDate)
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
    }

    @Test
    fun `범위에 오늘까지의 날짜가 없으면 줄을 만들지 않는다`() {
        // given
        val (patch1103, patch1111, patch1120) =
            savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 20))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOrNull(patch1120)).isNull()
        assertThat(resultCheckpointOf(patch1103).lastReadBattleId).isPositive
        val checkpoint1111 = resultCheckpointOf(patch1111)
        assertThat(checkpoint1111.appliedEndDate).isEqualTo(LocalDate.of(2026, 11, 19))
        assertThat(assertResultStats(patch1111, PatchDateRange(range1111.startDate, LocalDate.of(2026, 11, 19)))).isNotEmpty
    }

    @Test
    fun `저장된 지 10분이 안 된 전투는 다음 실행으로 미룬다`() {
        // given
        val (_, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        val old = saveResult(LocalDate.of(2026, 11, 13))
        val fresh = saveResult(LocalDate.of(2026, 11, 14), createdAt = now.minus(Duration.ofMinutes(5)), trophies = 2500)

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOf(patch1111).lastReadBattleId).isEqualTo(old.id)
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty

        // when
        task(Clock.fixed(now.plus(Duration.ofMinutes(20)), KOREA_ZONE)).execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOf(patch1111).lastReadBattleId).isEqualTo(fresh.id)
        assertResultStats(patch1111, range1111)
    }

    @Test
    fun `상한 id 가 없으면 읽지 않고 체크포인트를 그대로 둔다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        val before1103 = resultCheckpointOf(patch1103)
        val before1111 = resultCheckpointOf(patch1111)
        saveResult(LocalDate.of(2026, 11, 14))

        // when
        task(Clock.fixed(now.plus(Duration.ofDays(9)), KOREA_ZONE)).execute(aggregations.brawlerBattleResult)

        // then
        listOf(before1103 to resultCheckpointOf(patch1103), before1111 to resultCheckpointOf(patch1111)).forEach { (before, after) ->
            assertThat(after.lastReadBattleId).isEqualTo(before.lastReadBattleId)
            assertThat(after.version).isEqualTo(before.version)
            assertThat(after.lastBattleAt).isEqualTo(before.lastBattleAt)
        }
        assertResultStats(patch1111, range1111)
    }

    @Test
    fun `상한 id 가 체크포인트보다 작으면 읽지 않고 체크포인트를 내리지 않는다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        val cap = saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        val raised = cap.id!! + 100
        jdbcTemplate.update(
            "update stats_checkpoint set last_read_battle_id = ? where stats_checkpoint_id = ?",
            raised,
            resultCheckpointOf(patch1111).id,
        )
        val before = resultCheckpointOf(patch1111)
        val statsBefore = resultStatsOf(patch1111)

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        val after = resultCheckpointOf(patch1111)
        assertThat(after.lastReadBattleId).isEqualTo(raised)
        assertThat(after.version).isEqualTo(before.version)
        assertThat(statsBefore).isNotEmpty
        assertThat(resultStatsOf(patch1111)).isEqualTo(statsBefore)
        assertThat(resultCheckpointOf(patch1103).lastReadBattleId).isEqualTo(cap.id)
    }

    @Test
    fun `맞추기 뒤에도 적용한 범위가 지금의 범위와 다르면 실패하고 아무것도 바꾸지 않는다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        patchJpaRepository.save(
            patchJpaRepository.findById(patch1111).orElseThrow().apply { update(LocalDate.of(2026, 11, 12), null) },
        )
        saveResult(LocalDate.of(2026, 11, 14))
        val checkpointsBefore = listOf(patch1103, patch1111).map { resultCheckpointOf(it).snapshot() }
        val statsBefore = listOf(patch1103, patch1111).map { resultStatsOf(it) }
        val idleAligner = mockk<PatchStatsRangeAligner>(relaxUnitFun = true)

        // when & then
        assertThatThrownBy { task(rangeAligner = idleAligner).execute(aggregations.brawlerBattleResult) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("statsType=BRAWLER_BATTLE_RESULT")
            .hasMessageContaining("patchId=$patch1103")
        assertThat(listOf(patch1103, patch1111).map { resultCheckpointOf(it).snapshot() }).isEqualTo(checkpointsBefore)
        assertThat(listOf(patch1103, patch1111).map { resultStatsOf(it) }).isEqualTo(statsBefore)
    }

    @Test
    fun `실행 도중에 시각이 흘러도 시작할 때의 오늘과 상한으로 판단한다`() {
        // given
        val (_, patch1111, patch1120) =
            savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 20))
        saveResult(LocalDate.of(2026, 11, 4))
        val old = saveResult(LocalDate.of(2026, 11, 13))
        val late =
            saveResult(
                LocalDate.of(2026, 11, 19),
                createdAt = OffsetDateTime.parse("2026-11-20T23:55:00+09:00").toInstant(),
                trophies = 2500,
            )
        val clock =
            SteppingClock(
                first = OffsetDateTime.parse("2026-11-20T23:59:00+09:00").toInstant(),
                rest = OffsetDateTime.parse("2026-11-21T00:21:00+09:00").toInstant(),
            )

        // when
        task(clock).execute(aggregations.brawlerBattleResult)

        // then
        assertThat(resultCheckpointOrNull(patch1120)).isNull()
        val checkpoint1111 = resultCheckpointOf(patch1111)
        assertThat(checkpoint1111.lastReadBattleId).isEqualTo(old.id)
        assertThat(checkpoint1111.lastReadBattleId).isLessThan(late.id)
        assertThat(assertResultStats(patch1111, PatchDateRange(range1111.startDate, LocalDate.of(2026, 11, 19)))).isNotEmpty
    }

    @Test
    fun `더한 전투의 가장 늦은 일시가 최근 전투 일시가 된다`() {
        // given
        val (_, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        val latest = OffsetDateTime.parse("2026-11-17T21:30:00+09:00").toInstant()
        saveResult(LocalDate.of(2026, 11, 13))
        saveResult(LocalDate.of(2026, 11, 17), battleAt = latest)
        saveResult(LocalDate.of(2026, 11, 14))
        saveResult(LocalDate.of(2026, 11, 15))
        saveResult(LocalDate.of(2026, 11, 12))
        saveResult(LocalDate.of(2026, 11, 16))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        val checkpoint = resultCheckpointOf(patch1111)
        assertThat(checkpoint.version).isEqualTo(2L)
        assertThat(checkpoint.lastBattleAt).isEqualTo(latest)
        assertThat(assertResultStats(patch1111, range1111)).isNotEmpty
    }

    @Test
    fun `새 전투가 없으면 최근 전투 일시는 그대로다`() {
        // given
        val (patch1103, _) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        val battle1104 = saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregations.brawlerBattleResult)
        assertThat(resultCheckpointOf(patch1103).lastBattleAt).isEqualTo(battle1104.battleAt)
        saveResult(LocalDate.of(2026, 11, 14))

        // when
        task().execute(aggregations.brawlerBattleResult)

        // then
        val checkpoint = resultCheckpointOf(patch1103)
        assertThat(checkpoint.version).isEqualTo(2L)
        assertThat(checkpoint.lastBattleAt).isEqualTo(battle1104.battleAt)
    }

    @Test
    fun `다섯 유형이 각각 자기 테이블에 쓴다`() {
        // given
        val (patch1103, patch1111) = savePatches(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 11))
        saveResult(LocalDate.of(2026, 11, 4))
        saveRank(LocalDate.of(2026, 11, 4), battleRank = 1)
        saveResult(LocalDate.of(2026, 11, 6), result = BattleResult.DEFEAT.code, trophies = 2500)
        saveRank(LocalDate.of(2026, 11, 7), battleRank = 3)
        saveResult(LocalDate.of(2026, 11, 13))
        saveRank(LocalDate.of(2026, 11, 13), battleRank = 2, trophies = 2500)
        saveResult(LocalDate.of(2026, 11, 15), result = BattleResult.DRAW.code)
        saveRank(LocalDate.of(2026, 11, 15), battleRank = 10)

        // when
        task().execute(aggregations.brawlerBattleResult)
        task().execute(aggregations.brawlerBattleRank)
        task().execute(aggregations.brawlerEnemyBattleResult)
        task().execute(aggregations.brawlerPairBattleResult)
        task().execute(aggregations.brawlerTrioBattleResult)

        // then
        listOf(patch1103 to range1103, patch1111 to range1111).forEach { (patchId, range) ->
            assertTable(
                StatsType.BRAWLER_BATTLE_RESULT,
                resultTable,
                BrawlerBattleResultPatchStatsAggregator(),
                savedResults,
                patchId,
                range,
            )
            assertTable(StatsType.BRAWLER_BATTLE_RANK, rankTable, BrawlerBattleRankPatchStatsAggregator(), savedRanks, patchId, range)
            assertTable(
                StatsType.BRAWLER_ENEMY_BATTLE_RESULT,
                enemyTable,
                BrawlerEnemyBattleResultPatchStatsAggregator(),
                savedResults,
                patchId,
                range,
            )
            assertTable(
                StatsType.BRAWLER_PAIR_BATTLE_RESULT,
                pairTable,
                BrawlerPairBattleResultPatchStatsAggregator(),
                savedResults,
                patchId,
                range,
            )
            assertTable(
                StatsType.BRAWLER_TRIO_BATTLE_RESULT,
                trioTable,
                BrawlerTrioBattleResultPatchStatsAggregator(),
                savedResults,
                patchId,
                range,
            )
        }
        assertThat(checkpointOf(StatsType.BRAWLER_BATTLE_RANK, patch1111).lastReadBattleId).isEqualTo(savedRanks.last().id)
        assertThat(checkpointOf(StatsType.BRAWLER_TRIO_BATTLE_RESULT, patch1111).lastReadBattleId).isEqualTo(savedResults.last().id)
    }

    /** [assertStats] 에 더해 통계 줄이 실제로 있는지(빈 것끼리 같아 통과하지 않는지) 본다. */
    private fun <TBattle : BaseStatsBattleEntity> assertTable(
        statsType: StatsType,
        table: PatchStatsTable,
        aggregator: PatchStatsAggregator<TBattle>,
        saved: List<TBattle>,
        patchId: Long,
        range: PatchDateRange,
    ) {
        val actual = assertStats(statsType, table, aggregator, saved, patchId, range)
        assertThat(actual).describedAs("${table.name} patch=$patchId").isNotEmpty
    }

    private fun task(
        clock: Clock = Clock.fixed(now, KOREA_ZONE),
        rangeAligner: PatchStatsRangeAligner = this.rangeAligner,
    ): PatchStatsTask =
        PatchStatsTask(
            patchJpaRepository = patchJpaRepository,
            checkpointJpaRepository = checkpointJpaRepository,
            writer = writer,
            rangeAligner = rangeAligner,
            alertManager = mockk(relaxUnitFun = true),
            clock = clock,
            checkpointBattleCount = 4,
        )

    /** 1인 승률 통계가 "범위 안이고 id 가 체크포인트 이하"인 저장한 전투를 처음부터 집계한 값과 같은지 보고, 통계를 준다. */
    private fun assertResultStats(
        patchId: Long,
        range: PatchDateRange,
    ): Map<List<String>, List<Long>> =
        assertStats(StatsType.BRAWLER_BATTLE_RESULT, resultTable, BrawlerBattleResultPatchStatsAggregator(), savedResults, patchId, range)

    private fun <TBattle : BaseStatsBattleEntity> assertStats(
        statsType: StatsType,
        table: PatchStatsTable,
        aggregator: PatchStatsAggregator<TBattle>,
        saved: List<TBattle>,
        patchId: Long,
        range: PatchDateRange,
    ): Map<List<String>, List<Long>> {
        val checkpoint = checkpointOf(statsType, patchId)
        assertThat(PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate)).isEqualTo(range)
        val battles = saved.filter { range.contains(it.battleDate) && idOf(it) <= checkpoint.lastReadBattleId }
        val actual = support.statsOf(table.name, table.keyColumns, table.countColumns, patchId)
        assertThat(actual).describedAs("${table.name} patch=$patchId").isEqualTo(support.expectedOf(aggregator, battles))
        return actual
    }

    private fun idOf(battle: BaseStatsBattleEntity): Long =
        when (battle) {
            is StatsResultBattleEntity -> battle.id!!
            is StatsRankBattleEntity -> battle.id!!
            else -> error("모르는 원본 전투: ${battle::class}")
        }

    private fun resultStatsOf(patchId: Long): Map<List<String>, List<Long>> =
        support.statsOf(resultTable.name, resultTable.keyColumns, resultTable.countColumns, patchId)

    private fun StatsCheckpointEntity.snapshot() = listOf(id, lastReadBattleId, appliedStartDate, appliedEndDate, lastBattleAt, version)

    private fun resultCheckpointOf(patchId: Long): StatsCheckpointEntity = checkpointOf(StatsType.BRAWLER_BATTLE_RESULT, patchId)

    private fun resultCheckpointOrNull(patchId: Long): StatsCheckpointEntity? =
        checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT).find { it.brawlStarsPatchId == patchId }

    private fun checkpointOf(
        statsType: StatsType,
        patchId: Long,
    ): StatsCheckpointEntity = checkpointJpaRepository.findAllByStatsType(statsType).single { it.brawlStarsPatchId == patchId }

    private fun savePatches(vararg patchDates: LocalDate): List<Long> =
        patchDates.map { patchJpaRepository.save(BrawlStarsPatchEntity(it, null)).id!! }

    /** 3대3 결과형 전투. 상대는 2000 이상이라 봇 전투가 아니다. */
    private fun saveResult(
        date: LocalDate,
        createdAt: Instant = defaultCreatedAt,
        trophies: Int = 1500,
        result: String = BattleResult.VICTORY.code,
        battleAt: Instant = noonOf(date),
    ): StatsResultBattleEntity {
        check(battleAt.atZone(KOREA_ZONE).toLocalDate() == date)
        val battle =
            threeVsThree(
                collectorTeam = List(3) { trophies },
                opponents = List(3) { 2100 },
                result = result,
                battleKey = "k${++battleKeySeq}",
                battleAt = battleAt,
            )
        return support.saveResultBattle(battle, createdAt).also { savedResults.add(it) }
    }

    /** 솔로 쇼다운 순위형 전투. 다른 아홉 팀은 2000 이상이라 봇 로비가 아니다. */
    private fun saveRank(
        date: LocalDate,
        battleRank: Int,
        trophies: Int = 1500,
    ): StatsRankBattleEntity {
        val battle =
            showdown(
                modeType = StatsEventModeType.SOLO_SHOWDOWN,
                teamTrophies = listOf(listOf(trophies)) + List(9) { listOf(2100) },
                battleRank = battleRank,
                battleKey = "k${++battleKeySeq}",
                battleAt = noonOf(date),
            )
        return support.saveRankBattle(battle, defaultCreatedAt).also { savedRanks.add(it) }
    }

    private fun noonOf(date: LocalDate): Instant = OffsetDateTime.parse("${date}T12:00:00+09:00").toInstant()

    /** 진짜 읽기를 감싸, failingPage 번째 쪽(1부터, 모든 read 호출을 통틀어 센다)을 넘기기 전에 실패한다. */
    private class FailingOnPageBattleReader<TBattle : BaseStatsBattleEntity>(
        private val delegate: StatsBattleReader<TBattle>,
        private val failingPage: Int,
    ) : StatsBattleReader<TBattle> by delegate {
        private var page = 0

        override fun read(
            dateRange: PatchDateRange,
            afterBattleId: Long,
            battleIdCap: Long,
            consume: (List<TBattle>) -> Unit,
        ) {
            delegate.read(dateRange, afterBattleId, battleIdCap) { battles ->
                check(++page != failingPage) { "$failingPage 번째 쪽에서 끊는다" }
                consume(battles)
            }
        }
    }

    /** 처음 부를 때는 first, 그 뒤로는 rest 를 준다. */
    private class SteppingClock(
        private val first: Instant,
        private val rest: Instant,
    ) : Clock() {
        private var called = false

        override fun getZone(): ZoneId = KOREA_ZONE

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant =
            if (called) {
                rest
            } else {
                called = true
                first
            }
    }
}
