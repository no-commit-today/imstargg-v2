package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.StatsBotMonitor
import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsCheckpointJpaRepository
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * 통계 유형 하나의 한 번의 실행. 체크포인트 뒤에 새로 들어온 원본 전투를 패치마다 통계에 더한다.
 *
 * ## 한 실행 안에서 고정하는 값
 * 시각은 처음 한 번만 읽는다. 오늘(KST)과 상한 id 가 모두 그 시각에서 나오므로, 실행 도중 자정이나
 * 10분의 여유를 넘겨도 판단이 바뀌지 않는다. 상한 id 는 실행 시작 10분 전까지 저장된 원본 중 가장 큰 id 이고,
 * 찾는 날짜를 최근 8일로 좁힌다(적재가 받는 가장 오래된 전투가 7일 전이다).
 *
 * ## 평소 읽기 앞에서 하는 일
 * 1. 어드민에서 지운 패치의 통계 줄과 체크포인트 줄을 지운다.
 * 2. 적용한 범위가 지금의 범위와 다른 줄을 [PatchStatsRangeAligner] 로 하루씩 맞춘다. 끝나면 체크포인트 줄을 다시 읽는다.
 *    맞추기로 줄이 지워진 패치는 아래의 줄 만들기 규칙을 그대로 탄다.
 *
 * ## 평소 읽기
 * - 체크포인트 줄이 없는 패치는 줄을 만든다. 범위가 비었거나, 범위에 오늘까지의 날짜가 없거나,
 *   범위의 첫 날이 원본에 남은 가장 이른 날짜보다 이르면 만들지 않는다(앞부분이 빠진 통계가 온전해 보이지 않게).
 * - 줄의 적용한 범위가 지금의 범위와 다르면 실패한다. 섞어 읽으면 새로 든 날의 전투를 두 번 더할 수 있다.
 *   맞추기가 정상으로 끝나면 닿지 않는 자리이고, 닿았다면 정렬기의 버그나 바깥의 개입이라 조용히 넘기지 않는다.
 * - 읽은 전투가 [checkpointBattleCount] 이상 쌓이면 저장하고 체크포인트를 그 쪽의 마지막 id 로 올린다.
 *   다 읽으면 남은 것을 저장하며 체크포인트를 상한 id 로 올린다. 새 전투가 없어도 올린다.
 *
 * ## 봇 감시
 * 감시를 만드는 자리가 있는 유형은 실행마다 감시 하나를 만들어 평소 읽기에서 읽은 전투만 넣는다(범위 맞추기로 다시 읽는
 * 전투는 넣지 않는다). 실행 끝에 한 번, 넣은 전투가 있으면 요약 로그를 남기고 경고 지표가 있으면 알림을 한 번 보낸다.
 * 이 구간의 예외는 로그만 남기고 잡을 실패시키지 않는다(`DiscordAlertManager` 는 전송 실패를 예외로 던진다).
 *
 * 읽기는 트랜잭션 밖에서 하고, 쓰기는 [PatchStatsWriter] 가 저장마다 자기 트랜잭션을 연다. 그래서 이 클래스에는
 * 트랜잭션을 걸지 않는다. 예외는 잡지 않는다. 잡이 그 자리에서 실패하고 다음 실행이 체크포인트에서 잇는다.
 */
class PatchStatsTask(
    private val patchJpaRepository: BrawlStarsPatchJpaRepository,
    private val checkpointJpaRepository: StatsCheckpointJpaRepository,
    private val writer: PatchStatsWriter,
    private val rangeAligner: PatchStatsRangeAligner,
    private val alertManager: AlertManager,
    private val clock: Clock,
    private val checkpointBattleCount: Long,
) {
    fun <TBattle : BaseStatsBattleEntity> execute(aggregation: PatchStatsAggregation<TBattle>) {
        val now = clock.instant()
        val today = now.toKoreaLocalDate()
        val battleIdCap =
            aggregation.reader.findBattleIdCap(
                createdAtCap = now.minus(PatchStatsConstants.BATTLE_ID_CAP_MARGIN),
                startBattleDate = today.minusDays(PatchStatsConstants.BATTLE_ID_CAP_BATTLE_DAYS),
            )
        log.info { "패치 통계 실행 시작. statsType=${aggregation.statsType}, today=$today, battleIdCap=$battleIdCap" }

        val patches = patchJpaRepository.findAll().sortedBy { it.patchDate }
        val dateRanges = PatchDateRange.resolveAll(patches.associate { it.id!! to it.patchDate })
        deleteDeletedPatches(aggregation = aggregation, dateRanges = dateRanges)
        alignRanges(aggregation = aggregation, dateRanges = dateRanges, today = today)
        val checkpoints = checkpointJpaRepository.findAllByStatsType(aggregation.statsType).associateBy { it.brawlStarsPatchId }
        val earliestBattleDate by lazy { aggregation.reader.findEarliestBattleDate() }
        val monitor = aggregation.createBotMonitor?.invoke()
        var monitoredBattleCount = 0L

        for (patch in patches) {
            val patchId = patch.id!!
            val dateRange = dateRanges.getValue(patchId)
            val checkpoint =
                checkpoints[patchId]
                    ?: createCheckpoint(
                        aggregation = aggregation,
                        brawlStarsPatchId = patchId,
                        dateRange = dateRange,
                        today = today,
                        earliestBattleDate = earliestBattleDate,
                    )
                    ?: continue
            val appliedRange = PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate)
            check(appliedRange == dateRange) {
                "맞추기 뒤에도 적용한 범위가 지금의 범위와 다르다. statsType=${aggregation.statsType}, patchId=$patchId, " +
                    "appliedRange=$appliedRange, dateRange=$dateRange"
            }
            monitoredBattleCount +=
                readNewBattles(
                    aggregation = aggregation,
                    checkpoint = checkpoint,
                    dateRange = dateRange,
                    battleIdCap = battleIdCap,
                    monitor = monitor,
                )
        }

        if (monitor != null && monitoredBattleCount > 0) {
            reportBotMonitor(jobName = aggregation.jobName, monitor = monitor)
        }
    }

    /** 요약 로그를 남기고 경고 지표가 있으면 알림을 한 번 보낸다. 예외는 로그만 남긴다. */
    private fun reportBotMonitor(
        jobName: String,
        monitor: StatsBotMonitor<*>,
    ) {
        try {
            log.info { "[$jobName] 봇 감시 - ${monitor.summary()}" }
            val warnings = monitor.metrics().filter { it.isWarning }
            if (warnings.isNotEmpty()) {
                alertManager.alert(
                    AlertCommand
                        .builder()
                        .warn()
                        .title("[$jobName] 봇 전투 규칙 이상 감지")
                        .content(warnings.joinToString("\n") { it.describe() })
                        .build(),
                )
            }
        } catch (ex: Exception) {
            log.error(ex) { "[$jobName] 봇 감시 요약 실패" }
        }
    }

    /** 어드민에서 지운 패치(체크포인트 줄은 있는데 패치 목록에 없는 패치)의 통계 줄과 체크포인트 줄을 지운다. */
    private fun deleteDeletedPatches(
        aggregation: PatchStatsAggregation<*>,
        dateRanges: Map<Long, PatchDateRange>,
    ) {
        checkpointJpaRepository
            .findAllByStatsType(aggregation.statsType)
            .filter { it.brawlStarsPatchId !in dateRanges }
            .forEach { checkpoint ->
                writer.deletePatch(
                    table = aggregation.table,
                    brawlStarsPatchId = checkpoint.brawlStarsPatchId,
                    checkpointId = checkpoint.id!!,
                )
                log.info { "지워진 패치의 통계를 지웠다. statsType=${aggregation.statsType}, patchId=${checkpoint.brawlStarsPatchId}" }
            }
    }

    /** 적용한 범위가 지금의 범위와 다른 줄을 하루씩 맞춘다. 범위가 다 빈 줄은 지워진다. */
    private fun <TBattle : BaseStatsBattleEntity> alignRanges(
        aggregation: PatchStatsAggregation<TBattle>,
        dateRanges: Map<Long, PatchDateRange>,
        today: LocalDate,
    ) {
        checkpointJpaRepository.findAllByStatsType(aggregation.statsType).forEach { checkpoint ->
            val dateRange = dateRanges.getValue(checkpoint.brawlStarsPatchId)
            if (PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate) != dateRange) {
                rangeAligner.align(aggregation = aggregation, checkpoint = checkpoint, dateRange = dateRange, today = today)
            }
        }
    }

    /** 줄을 만들고 다시 읽어 준다. 만들지 않는 경우에는 null. */
    private fun createCheckpoint(
        aggregation: PatchStatsAggregation<*>,
        brawlStarsPatchId: Long,
        dateRange: PatchDateRange,
        today: LocalDate,
        earliestBattleDate: LocalDate?,
    ): StatsCheckpointEntity? {
        if (dateRange.isEmpty || dateRange.startDate > today) return null
        if (earliestBattleDate == null || earliestBattleDate > dateRange.startDate) return null

        writer.createCheckpoint(statsType = aggregation.statsType, brawlStarsPatchId = brawlStarsPatchId, dateRange = dateRange)
        log.info { "체크포인트 줄을 만들었다. statsType=${aggregation.statsType}, patchId=$brawlStarsPatchId, dateRange=$dateRange" }
        return checkpointJpaRepository
            .findAllByStatsType(aggregation.statsType)
            .single { it.brawlStarsPatchId == brawlStarsPatchId }
    }

    /** 읽은 전투 수를 준다. 읽지 않았으면 0. 읽은 전투는 모두 monitor 에 넣는다. */
    private fun <TBattle : BaseStatsBattleEntity> readNewBattles(
        aggregation: PatchStatsAggregation<TBattle>,
        checkpoint: StatsCheckpointEntity,
        dateRange: PatchDateRange,
        battleIdCap: Long?,
        monitor: StatsBotMonitor<TBattle>?,
    ): Long {
        if (battleIdCap == null || battleIdCap <= checkpoint.lastReadBattleId) return 0L

        var version = checkpoint.version
        var lastBattleAt = checkpoint.lastBattleAt
        var aggregator = aggregation.createAggregator()
        var unsavedBattleCount = 0L
        var unsavedLastBattleAt: Instant? = null
        var readBattleCount = 0L
        var saveCount = 0

        fun save(lastReadBattleId: Long) {
            lastBattleAt = listOfNotNull(lastBattleAt, unsavedLastBattleAt).maxOrNull()
            writer.add(
                table = aggregation.table,
                brawlStarsPatchId = checkpoint.brawlStarsPatchId,
                checkpoint =
                    StatsCheckpointUpdate(
                        checkpointId = checkpoint.id!!,
                        version = version,
                        lastReadBattleId = lastReadBattleId,
                        appliedStartDate = dateRange.startDate,
                        appliedEndDate = dateRange.endDate,
                        lastBattleAt = lastBattleAt,
                    ),
                counts = aggregator.toCounts(),
            )
            version++
            saveCount++
            aggregator = aggregation.createAggregator()
            unsavedBattleCount = 0L
            unsavedLastBattleAt = null
        }

        aggregation.reader.read(
            dateRange = dateRange,
            afterBattleId = checkpoint.lastReadBattleId,
            battleIdCap = battleIdCap,
        ) { battles ->
            battles.forEach {
                aggregator.aggregate(it)
                monitor?.record(it)
            }
            unsavedBattleCount += battles.size
            readBattleCount += battles.size
            unsavedLastBattleAt = listOfNotNull(unsavedLastBattleAt, battles.maxOf { it.battleAt }).max()
            if (unsavedBattleCount >= checkpointBattleCount) {
                save(lastReadBattleId = battleIdOf(battles.last()))
            }
        }
        save(lastReadBattleId = battleIdCap)

        log.info {
            "패치 통계를 더했다. statsType=${aggregation.statsType}, patchId=${checkpoint.brawlStarsPatchId}, " +
                "readBattleCount=$readBattleCount, saveCount=$saveCount, lastReadBattleId=$battleIdCap"
        }
        return readBattleCount
    }

    // 원본 엔티티의 id 는 하위 클래스마다 따로 있어(BaseStatsBattleEntity 에는 없다) 유형으로 가른다.
    private fun battleIdOf(battle: BaseStatsBattleEntity): Long =
        when (battle) {
            is StatsResultBattleEntity -> checkNotNull(battle.id)
            is StatsRankBattleEntity -> checkNotNull(battle.id)
            else -> error("패치 통계가 읽지 않는 원본 전투다. type=${battle::class.qualifiedName}")
        }
}
