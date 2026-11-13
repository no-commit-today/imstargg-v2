package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.StatsBotMonitor
import com.imstargg.core.batch.domain.statistics.StatsBotMonitorMetric
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.StatsCheckpointJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class PatchStatsBotMonitorTest(
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
    private val base =
        PatchStatsAggregations(
            StatsResultBattleReader(entityManagerFactory, pageSize = 2),
            StatsRankBattleReader(entityManagerFactory, pageSize = 2),
        ).brawlerBattleResult

    private val now: Instant = OffsetDateTime.parse("2026-11-20T10:00:00+09:00").toInstant()
    private val alertManager = mockk<AlertManager>(relaxUnitFun = true)
    private val savedResults = mutableListOf<StatsResultBattleEntity>()
    private var battleKeySeq = 0

    private val warningMetric = StatsBotMonitorMetric(name = "걸린 전투 승률", count = 0, total = 1000, warnBelow = 0.9, minSample = 1)
    private val calmMetric = StatsBotMonitorMetric(name = "걸린 전투 승률", count = 1000, total = 1000, warnBelow = 0.9, minSample = 1)

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `평소 읽기에서 읽은 전투만 감시에 넣는다`() {
        // given
        (4..19).forEach { saveResult(LocalDate.of(2026, 11, it)) }
        savePatch(LocalDate.of(2026, 11, 3))
        task().execute(aggregation(RecordingMonitor(listOf(calmMetric))))
        savePatch(LocalDate.of(2026, 11, 11))
        val monitor = RecordingMonitor(listOf(calmMetric))

        // when
        task().execute(aggregation(monitor))

        // then
        // 범위 맞추기는 앞 패치에서 11일부터의 전투를 다시 읽어 빼지만, 감시에는 새 패치를 처음부터 채운 평소 읽기의 전투만 들어간다.
        val readInRegularRead = savedResults.filter { it.battleDate >= LocalDate.of(2026, 11, 12) }
        assertThat(readInRegularRead).isNotEmpty
        assertThat(monitor.recorded.map { it.id }).containsExactlyInAnyOrderElementsOf(readInRegularRead.map { it.id })
        assertThat(monitor.recorded.size).isLessThan(savedResults.size)
    }

    @Test
    fun `경고 지표가 있으면 실행당 한 번 알린다`() {
        // given
        savePatch(LocalDate.of(2026, 11, 3))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        saveResult(LocalDate.of(2026, 11, 14))
        val monitor = RecordingMonitor(listOf(warningMetric, calmMetric))
        val command = slot<AlertCommand>()

        // when
        task().execute(aggregation(monitor))

        // then
        verify(exactly = 1) { alertManager.alert(capture(command)) }
        assertThat(command.captured.title).endsWith("[testPatchStatsJob] 봇 전투 규칙 이상 감지")
        assertThat(command.captured.content).isEqualTo(warningMetric.describe())
        assertThat(monitor.summaryCount).isEqualTo(1)
    }

    @Test
    fun `경고 지표가 없으면 알리지 않는다`() {
        // given
        savePatch(LocalDate.of(2026, 11, 3))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        val monitor = RecordingMonitor(listOf(calmMetric))

        // when
        task().execute(aggregation(monitor))

        // then
        verify(exactly = 0) { alertManager.alert(any()) }
        assertThat(monitor.summaryCount).isEqualTo(1)
    }

    @Test
    fun `읽은 전투가 없으면 요약도 알림도 없다`() {
        // given
        savePatch(LocalDate.of(2026, 11, 3))
        saveResult(LocalDate.of(2026, 11, 4))
        saveResult(LocalDate.of(2026, 11, 13))
        task().execute(aggregation(RecordingMonitor(listOf(calmMetric))))
        val monitor = RecordingMonitor(listOf(warningMetric))

        // when
        task().execute(aggregation(monitor))

        // then
        assertThat(monitor.recorded).isEmpty()
        assertThat(monitor.summaryCount).isEqualTo(0)
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `알림이 실패해도 잡은 실패하지 않는다`() {
        // given
        val patchId = savePatch(LocalDate.of(2026, 11, 3))
        saveResult(LocalDate.of(2026, 11, 4))
        val last = saveResult(LocalDate.of(2026, 11, 13))
        every { alertManager.alert(any()) } throws IllegalStateException("전송 실패")

        // when
        task().execute(aggregation(RecordingMonitor(listOf(warningMetric))))

        // then
        verify(exactly = 1) { alertManager.alert(any()) }
        val checkpoint = checkpointJpaRepository.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT).single()
        assertThat(checkpoint.brawlStarsPatchId).isEqualTo(patchId)
        assertThat(checkpoint.lastReadBattleId).isEqualTo(last.id)
    }

    private fun aggregation(monitor: StatsBotMonitor<StatsResultBattleEntity>): PatchStatsAggregation<StatsResultBattleEntity> =
        PatchStatsAggregation(
            statsType = base.statsType,
            jobName = "testPatchStatsJob",
            table = base.table,
            reader = base.reader,
            createAggregator = base.createAggregator,
            createBotMonitor = { monitor },
        )

    private fun task(): PatchStatsTask =
        PatchStatsTask(
            patchJpaRepository = patchJpaRepository,
            checkpointJpaRepository = checkpointJpaRepository,
            writer = writer,
            rangeAligner = rangeAligner,
            alertManager = alertManager,
            clock = Clock.fixed(now, KOREA_ZONE),
            checkpointBattleCount = 4,
        )

    private fun savePatch(patchDate: LocalDate): Long = patchJpaRepository.save(BrawlStarsPatchEntity(patchDate, null)).id!!

    private fun saveResult(date: LocalDate): StatsResultBattleEntity {
        val battle =
            threeVsThree(
                collectorTeam = List(3) { 1500 },
                opponents = List(3) { 2100 },
                battleKey = "k${++battleKeySeq}",
                battleAt = OffsetDateTime.parse("${date}T12:00:00+09:00").toInstant(),
            )
        return support.saveResultBattle(battle, now.minus(Duration.ofHours(1))).also { savedResults.add(it) }
    }

    private class RecordingMonitor(
        private val metrics: List<StatsBotMonitorMetric>,
    ) : StatsBotMonitor<StatsResultBattleEntity> {
        val recorded = mutableListOf<StatsResultBattleEntity>()
        var summaryCount = 0

        override fun record(battle: StatsResultBattleEntity) {
            recorded.add(battle)
        }

        override fun metrics(): List<StatsBotMonitorMetric> = metrics

        override fun summary(): String {
            summaryCount++
            return "테스트 요약"
        }
    }
}
