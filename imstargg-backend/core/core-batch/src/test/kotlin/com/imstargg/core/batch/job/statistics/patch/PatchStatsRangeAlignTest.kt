package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.patch.BrawlerBattleResultPatchStatsAggregator
import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
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

class PatchStatsRangeAlignTest(
    entityManagerFactory: EntityManagerFactory,
    jdbcTemplate: JdbcTemplate,
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
    private val aggregation =
        PatchStatsAggregations(
            resultReader,
            StatsRankBattleReader(entityManagerFactory, pageSize = 2),
        ).brawlerBattleResult

    private val now: Instant = OffsetDateTime.parse("2026-11-20T10:00:00+09:00").toInstant()
    private val defaultCreatedAt: Instant = now.minus(Duration.ofHours(1))

    private val savedResults = mutableListOf<StatsResultBattleEntity>()
    private var battleKeySeq = 0

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `패치를 등록하면 앞 패치에서 그날부터의 전투가 빠지고 새 패치가 채워진다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        assertStats(patch1103, PatchDateRange(date(4), null))

        // when
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), date(10)))).isNotEmpty
        assertThat(assertStats(patch1111, PatchDateRange(date(12), null))).isNotEmpty
        assertThat(savedResults.filter { it.battleDate == date(11) }).isNotEmpty
    }

    @Test
    fun `패치를 삭제하면 앞 패치가 끝 없음으로 넓어지고 지운 패치의 줄이 사라진다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)
        assertStats(patch1103, PatchDateRange(date(4), date(10)))

        // when
        patchJpaRepository.deleteById(patch1111)
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), null))).isNotEmpty
        assertThat(checkpointOrNull(patch1111)).isNull()
        assertThat(statsOf(patch1111)).isEmpty()
    }

    @Test
    fun `패치일을 뒤로 고치면 앞 패치는 넓어지고 그 패치는 줄어든다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)
        patchJpaRepository.save(
            patchJpaRepository.findById(patch1111).orElseThrow().apply { update(date(12), null) },
        )
        val added = saveResult(date(14), trophies = 2500, result = BattleResult.DRAW.code)

        // when
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), date(11)))).isNotEmpty
        assertThat(assertStats(patch1111, PatchDateRange(date(13), null))).isNotEmpty
        // 맞추기 뒤에 같은 실행에서 평소 읽기까지 이어져 새 전투가 들어갔다.
        assertThat(checkpointOf(patch1111).lastReadBattleId).isEqualTo(added.id)
        assertThat(checkpointOf(patch1103).lastReadBattleId).isEqualTo(added.id)
    }

    @Test
    fun `패치를 사이에 끼워 넣으면 앞 패치가 줄고 끼운 패치가 처음부터 채워진다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)

        // when
        val patch1107 = savePatch(date(7))
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), date(6)))).isNotEmpty
        assertThat(assertStats(patch1107, PatchDateRange(date(8), date(10)))).isNotEmpty
        assertThat(assertStats(patch1111, PatchDateRange(date(12), null))).isNotEmpty
    }

    @Test
    fun `맞추는 도중에 실패해도 다시 돌리면 결과가 같다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        val patch1111 = savePatch(date(11))
        val failingReader = FailingOnReadBattleReader(resultReader, failingRead = 3)
        val failingAggregation =
            PatchStatsAggregation(
                aggregation.statsType,
                aggregation.jobName,
                aggregation.table,
                failingReader,
                aggregation.createAggregator,
            )
        assertThatThrownBy { task().execute(failingAggregation) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("3 번째 읽기")
        val stopped = checkpointOf(patch1103)
        assertThat(PatchDateRange(stopped.appliedStartDate, stopped.appliedEndDate)).isEqualTo(PatchDateRange(date(4), date(18)))
        assertInvariant(patch1103)

        // when
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), date(10)))).isNotEmpty
        assertThat(assertStats(patch1111, PatchDateRange(date(12), null))).isNotEmpty
    }

    @Test
    fun `끝을 줄이면 최근 전투 일시가 새 끝 날짜의 다음 날 0시를 넘지 않는다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        assertThat(checkpointOf(patch1103).lastBattleAt).isAfter(OffsetDateTime.parse("2026-11-19T00:00:00+09:00").toInstant())

        // when
        savePatch(date(11))
        task().execute(aggregation)

        // then
        assertThat(checkpointOf(patch1103).lastBattleAt)
            .isBeforeOrEqualTo(OffsetDateTime.parse("2026-11-11T00:00:00+09:00").toInstant())
        assertStats(patch1103, PatchDateRange(date(4), date(10)))
    }

    @Test
    fun `더한 날의 전투가 더 늦으면 최근 전투 일시가 오른다`() {
        // given
        saveBattlesEveryDay()
        val latest = OffsetDateTime.parse("2026-11-19T21:30:00+09:00").toInstant()
        saveResult(date(19), battleAt = latest)
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)
        assertThat(checkpointOf(patch1103).lastBattleAt).isBefore(OffsetDateTime.parse("2026-11-11T00:00:00+09:00").toInstant())

        // when
        patchJpaRepository.deleteById(patch1111)
        task().execute(aggregation)

        // then
        assertThat(checkpointOf(patch1103).lastBattleAt).isEqualTo(latest)
        assertStats(patch1103, PatchDateRange(date(4), null))
    }

    @Test
    fun `범위가 통째로 비면 줄이 모두 사라지고 범위가 다시 생기면 처음부터 채운다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        assertThat(statsOf(patch1103)).isNotEmpty

        // when
        val patch1104 = savePatch(date(4))
        task().execute(aggregation)

        // then
        assertThat(statsOf(patch1103)).isEmpty()
        assertThat(checkpointOrNull(patch1103)).isNull()
        assertThat(assertStats(patch1104, PatchDateRange(date(5), null))).isNotEmpty

        // when
        patchJpaRepository.deleteById(patch1104)
        task().execute(aggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), null))).isNotEmpty
        assertThat(checkpointOrNull(patch1104)).isNull()
        assertThat(statsOf(patch1104)).isEmpty()
    }

    @Test
    fun `끝이 없던 줄은 오늘부터 거슬러 뺀다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        savePatch(date(11))
        val recordingReader = RecordingBattleReader(resultReader)
        val recordingAggregation =
            PatchStatsAggregation(
                aggregation.statsType,
                aggregation.jobName,
                aggregation.table,
                recordingReader,
                aggregation.createAggregator,
            )

        // when
        task().execute(recordingAggregation)

        // then
        val subtractedDays = (20 downTo 11).map { PatchDateRange(date(it), date(it)) }
        assertThat(recordingReader.dateRanges.take(subtractedDays.size)).isEqualTo(subtractedDays)
        assertStats(patch1103, PatchDateRange(date(4), date(10)))
    }

    @Test
    fun `어드민에서 지운 패치의 통계 줄과 체크포인트 줄을 지운다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)
        val before1111 = statsOf(patch1111)
        assertThat(statsOf(patch1103)).isNotEmpty

        // when
        patchJpaRepository.deleteById(patch1103)
        task().execute(aggregation)

        // then
        assertThat(checkpointOrNull(patch1103)).isNull()
        assertThat(statsOf(patch1103)).isEmpty()
        assertThat(before1111).isNotEmpty
        assertThat(assertStats(patch1111, PatchDateRange(date(12), null))).isEqualTo(before1111)
    }

    @Test
    fun `범위가 같은 패치는 다시 읽어 빼거나 더하지 않는다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        val patch1111 = savePatch(date(11))
        task().execute(aggregation)
        val before1103 = checkpointOf(patch1103).version
        val before1111 = checkpointOf(patch1111).version
        // 상한 id 가 체크포인트보다 커야 평소 읽기가 저장까지 간다.
        saveResult(date(14))
        val recordingReader = RecordingBattleReader(resultReader)
        val recordingAggregation =
            PatchStatsAggregation(
                aggregation.statsType,
                aggregation.jobName,
                aggregation.table,
                recordingReader,
                aggregation.createAggregator,
            )

        // when
        task().execute(recordingAggregation)

        // then
        assertThat(checkpointOf(patch1103).version).isEqualTo(before1103 + 1)
        assertThat(checkpointOf(patch1111).version).isEqualTo(before1111 + 1)
        assertThat(recordingReader.dateRanges)
            .containsExactly(PatchDateRange(date(4), date(10)), PatchDateRange(date(12), null))
        assertStats(patch1103, PatchDateRange(date(4), date(10)))
        assertStats(patch1111, PatchDateRange(date(12), null))
    }

    @Test
    fun `오늘까지 든 범위의 끝만 달라지면 더하거나 빼지 않고 끝만 고친다`() {
        // given
        saveBattlesEveryDay()
        val patch1103 = savePatch(date(3))
        task().execute(aggregation)
        val before = statsOf(patch1103)
        val recordingReader = RecordingBattleReader(resultReader)
        val recordingAggregation =
            PatchStatsAggregation(
                aggregation.statsType,
                aggregation.jobName,
                aggregation.table,
                recordingReader,
                aggregation.createAggregator,
            )

        // when: 끝 없음 → 오늘(11-20)에서 끝남
        val patch1121 = savePatch(date(21))
        task().execute(recordingAggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), date(20)))).isEqualTo(before)
        assertThat(checkpointOrNull(patch1121)).isNull()

        // when: 오늘에서 끝남 → 끝 없음
        patchJpaRepository.deleteById(patch1121)
        task().execute(recordingAggregation)

        // then
        assertThat(assertStats(patch1103, PatchDateRange(date(4), null))).isEqualTo(before)
        assertThat(recordingReader.dateRanges.filter { it.startDate == it.endDate }).isEmpty()
    }

    private fun task(clock: Clock = Clock.fixed(now, KOREA_ZONE)): PatchStatsTask =
        PatchStatsTask(
            patchJpaRepository = patchJpaRepository,
            checkpointJpaRepository = checkpointJpaRepository,
            writer = writer,
            rangeAligner = rangeAligner,
            alertManager = mockk(relaxUnitFun = true),
            clock = clock,
            checkpointBattleCount = 4,
        )

    /** 체크포인트 줄의 적용한 범위가 range 와 같고, 통계가 "range 안이고 id 가 체크포인트 이하"인 전투를 처음부터 집계한 값과 같은지 본다. */
    private fun assertStats(
        patchId: Long,
        range: PatchDateRange,
    ): Map<List<String>, List<Long>> {
        val checkpoint = checkpointOf(patchId)
        assertThat(PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate)).isEqualTo(range)
        return assertInvariant(patchId)
    }

    /** 통계가 체크포인트 줄의 적용한 범위와 체크포인트로 처음부터 집계한 값과 같은지 본다(지키는 규칙). */
    private fun assertInvariant(patchId: Long): Map<List<String>, List<Long>> {
        val checkpoint = checkpointOf(patchId)
        val range = PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate)
        val battles = savedResults.filter { range.contains(it.battleDate) && it.id!! <= checkpoint.lastReadBattleId }
        val actual = statsOf(patchId)
        assertThat(actual)
            .describedAs("patch=$patchId")
            .isEqualTo(support.expectedOf(BrawlerBattleResultPatchStatsAggregator(), battles))
        return actual
    }

    private fun statsOf(patchId: Long): Map<List<String>, List<Long>> =
        support.statsOf(aggregation.table.name, aggregation.table.keyColumns, aggregation.table.countColumns, patchId)

    private fun checkpointOf(patchId: Long): StatsCheckpointEntity =
        checkNotNull(checkpointOrNull(patchId)) { "체크포인트 줄이 없다. patch=$patchId" }

    private fun checkpointOrNull(patchId: Long): StatsCheckpointEntity? =
        checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT).find { it.brawlStarsPatchId == patchId }

    private fun savePatch(patchDate: LocalDate): Long = patchJpaRepository.save(BrawlStarsPatchEntity(patchDate, null)).id!!

    private fun date(dayOfMonth: Int): LocalDate = LocalDate.of(2026, 11, dayOfMonth)

    /** 11-04 부터 11-19 까지 날마다 두 건(승리 1500, 패배 2500)을 넣는다. */
    private fun saveBattlesEveryDay() {
        (4..19).forEach { day ->
            saveResult(date(day))
            saveResult(date(day), trophies = 2500, result = BattleResult.DEFEAT.code)
        }
    }

    /** 3대3 결과형 전투. 상대는 2000 이상이라 봇 전투가 아니다. */
    private fun saveResult(
        date: LocalDate,
        trophies: Int = 1500,
        result: String = BattleResult.VICTORY.code,
        battleAt: Instant = OffsetDateTime.parse("${date}T12:00:00+09:00").toInstant(),
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
        return support.saveResultBattle(battle, defaultCreatedAt).also { savedResults.add(it) }
    }

    /** 진짜 읽기를 감싸, read 를 부른 날짜 범위를 차례로 남긴다. */
    private class RecordingBattleReader(
        private val delegate: StatsBattleReader<StatsResultBattleEntity>,
    ) : StatsBattleReader<StatsResultBattleEntity> by delegate {
        val dateRanges = mutableListOf<PatchDateRange>()

        override fun read(
            dateRange: PatchDateRange,
            afterBattleId: Long,
            battleIdCap: Long,
            consume: (List<StatsResultBattleEntity>) -> Unit,
        ) {
            dateRanges.add(dateRange)
            delegate.read(dateRange, afterBattleId, battleIdCap, consume)
        }
    }

    /** 진짜 읽기를 감싸, failingRead 번째 read 호출(1부터)에서 읽기 전에 실패한다. */
    private class FailingOnReadBattleReader(
        private val delegate: StatsBattleReader<StatsResultBattleEntity>,
        private val failingRead: Int,
    ) : StatsBattleReader<StatsResultBattleEntity> by delegate {
        private var read = 0

        override fun read(
            dateRange: PatchDateRange,
            afterBattleId: Long,
            battleIdCap: Long,
            consume: (List<StatsResultBattleEntity>) -> Unit,
        ) {
            check(++read != failingRead) { "$failingRead 번째 읽기에서 끊는다. dateRange=$dateRange" }
            delegate.read(dateRange, afterBattleId, battleIdCap, consume)
        }
    }
}
