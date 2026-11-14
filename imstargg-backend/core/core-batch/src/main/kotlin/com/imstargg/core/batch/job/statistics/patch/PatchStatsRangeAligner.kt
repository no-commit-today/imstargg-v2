package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * 체크포인트 줄의 적용한 범위를 패치의 지금 범위에 하루씩 맞춘다.
 *
 * ## 지키는 규칙
 * 한 날을 옮기는 것이 트랜잭션 하나다. 그날의 전투 중 id 가 체크포인트 이하인 것을 다시 읽어 빼거나 더하고,
 * 같은 트랜잭션에서 적용한 범위를 하루 줄이거나 넓힌다. 그래서 어느 날 뒤에 멈춰도 "통계 = 적용한 범위 안이고
 * id 가 체크포인트 이하인 원본의 집계"가 성립하고, 다음 실행이 남은 날부터 잇는다. 체크포인트는 바꾸지 않는다.
 *
 * ## 순서
 * 줄이기를 먼저 하고 늘리기를 한다. 줄일 때는 바깥쪽 날부터, 늘릴 때는 범위에 붙은 날부터 해서 적용한 범위가
 * 늘 이어진 한 구간으로 남는다. 끝이 없던 범위의 마지막 날은 오늘(KST)이다.
 * 1. 끝 줄이기 → 2. 앞 줄이기. 빼려는 날이 남은 마지막 하루면 체크포인트 줄까지 지우고 끝낸다
 *    (범위가 다시 생기면 새 패치처럼 처음부터 읽는다).
 * 3. 앞 늘리기 → 4. 끝 늘리기. 지금 범위의 끝이 없으면 오늘까지 더하고, 오늘을 더하는 트랜잭션에서 끝을 비운다.
 *
 * ## 최근 전투 일시
 * 끝을 줄이면 새 끝 날짜의 다음 날 0시(KST)를 넘지 않게 내린다. 더하면 더한 전투의 가장 늦은 일시와 견줘 큰 값을 둔다.
 *
 * 읽기는 트랜잭션 밖에서 하고, 쓰기는 [PatchStatsWriter] 가 날마다 자기 트랜잭션을 연다. 예외는 잡지 않는다.
 */
@Component
class PatchStatsRangeAligner(
    private val writer: PatchStatsWriter,
) {
    /** 체크포인트 줄의 적용한 범위를 dateRange 에 하루씩 맞춘다. 범위가 다 비면 줄이 지워진다. */
    fun <TBattle : BaseStatsBattleEntity> align(
        aggregation: PatchStatsAggregation<TBattle>,
        checkpoint: StatsCheckpointEntity,
        dateRange: PatchDateRange,
        today: LocalDate,
    ) {
        log.info {
            "적용한 범위를 맞춘다. statsType=${aggregation.statsType}, patchId=${checkpoint.brawlStarsPatchId}, " +
                "applied=${PatchDateRange(checkpoint.appliedStartDate, checkpoint.appliedEndDate)}, dateRange=$dateRange, today=$today"
        }
        val state = AlignState(checkpoint)

        // 1. 끝 줄이기
        while (!dateRange.contains(state.lastDay(today))) {
            val day = state.lastDay(today)
            if (state.isLastDay(today)) {
                subtractLast(aggregation = aggregation, state = state, day = day)
                return
            }
            val newEndDate = day.minusDays(1)
            val ceiling = newEndDate.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant()
            subtract(
                aggregation = aggregation,
                state = state,
                day = day,
                appliedStartDate = state.startDate,
                appliedEndDate = newEndDate,
                lastBattleAt = state.lastBattleAt?.let { minOf(it, ceiling) },
            )
        }

        // 2. 앞 줄이기
        while (!dateRange.contains(state.startDate)) {
            val day = state.startDate
            if (state.isLastDay(today)) {
                subtractLast(aggregation = aggregation, state = state, day = day)
                return
            }
            subtract(
                aggregation = aggregation,
                state = state,
                day = day,
                appliedStartDate = day.plusDays(1),
                appliedEndDate = state.endDate,
                lastBattleAt = state.lastBattleAt,
            )
        }

        // 3. 앞 늘리기
        while (state.startDate > dateRange.startDate) {
            val day = state.startDate.minusDays(1)
            add(aggregation = aggregation, state = state, day = day, appliedStartDate = day, appliedEndDate = state.endDate)
        }

        // 4. 끝 늘리기
        val endDate = dateRange.endDate
        if (endDate != null) {
            while (state.lastDay(today) < endDate) {
                val day = state.lastDay(today).plusDays(1)
                add(aggregation = aggregation, state = state, day = day, appliedStartDate = state.startDate, appliedEndDate = day)
            }
            if (state.endDate == null) {
                // 끝이 없던 범위의 마지막 날(오늘)이 지금 범위의 끝과 같다. 더할 것 없이 끝만 적는다.
                changeEndDate(aggregation = aggregation, state = state, appliedEndDate = endDate)
            }
        } else if (state.endDate != null) {
            var day = state.lastDay(today).plusDays(1)
            while (day <= today) {
                add(
                    aggregation = aggregation,
                    state = state,
                    day = day,
                    appliedStartDate = state.startDate,
                    appliedEndDate = if (day == today) null else day,
                )
                day = day.plusDays(1)
            }
            if (state.endDate != null) {
                // 오늘까지 이미 들어 있었다. 더할 것 없이 끝만 비운다.
                changeEndDate(aggregation = aggregation, state = state, appliedEndDate = null)
            }
        }
        log.info { "적용한 범위를 맞췄다. statsType=${aggregation.statsType}, patchId=${checkpoint.brawlStarsPatchId}, dateRange=$dateRange" }
    }

    private fun <TBattle : BaseStatsBattleEntity> subtract(
        aggregation: PatchStatsAggregation<TBattle>,
        state: AlignState,
        day: LocalDate,
        appliedStartDate: LocalDate,
        appliedEndDate: LocalDate?,
        lastBattleAt: Instant?,
    ) {
        val read = readDay(aggregation = aggregation, state = state, day = day)
        writer.subtract(
            table = aggregation.table,
            brawlStarsPatchId = state.brawlStarsPatchId,
            checkpoint = state.update(appliedStartDate = appliedStartDate, appliedEndDate = appliedEndDate, lastBattleAt = lastBattleAt),
            counts = read.counts,
        )
        state.applied(appliedStartDate = appliedStartDate, appliedEndDate = appliedEndDate, lastBattleAt = lastBattleAt)
    }

    private fun <TBattle : BaseStatsBattleEntity> subtractLast(
        aggregation: PatchStatsAggregation<TBattle>,
        state: AlignState,
        day: LocalDate,
    ) {
        val read = readDay(aggregation = aggregation, state = state, day = day)
        writer.subtractLast(
            table = aggregation.table,
            brawlStarsPatchId = state.brawlStarsPatchId,
            checkpointId = state.checkpointId,
            version = state.version,
            counts = read.counts,
        )
        log.info { "범위가 다 비어 체크포인트 줄을 지웠다. statsType=${aggregation.statsType}, patchId=${state.brawlStarsPatchId}" }
    }

    private fun <TBattle : BaseStatsBattleEntity> add(
        aggregation: PatchStatsAggregation<TBattle>,
        state: AlignState,
        day: LocalDate,
        appliedStartDate: LocalDate,
        appliedEndDate: LocalDate?,
    ) {
        val read = readDay(aggregation = aggregation, state = state, day = day)
        val lastBattleAt = listOfNotNull(state.lastBattleAt, read.lastBattleAt).maxOrNull()
        writer.add(
            table = aggregation.table,
            brawlStarsPatchId = state.brawlStarsPatchId,
            checkpoint = state.update(appliedStartDate = appliedStartDate, appliedEndDate = appliedEndDate, lastBattleAt = lastBattleAt),
            counts = read.counts,
        )
        state.applied(appliedStartDate = appliedStartDate, appliedEndDate = appliedEndDate, lastBattleAt = lastBattleAt)
    }

    /** 통계 줄을 건드리지 않고 적용한 끝만 고친다. 범위에 드는 날짜는 그대로다. */
    private fun changeEndDate(
        aggregation: PatchStatsAggregation<*>,
        state: AlignState,
        appliedEndDate: LocalDate?,
    ) {
        writer.add(
            table = aggregation.table,
            brawlStarsPatchId = state.brawlStarsPatchId,
            checkpoint =
                state.update(
                    appliedStartDate = state.startDate,
                    appliedEndDate = appliedEndDate,
                    lastBattleAt = state.lastBattleAt,
                ),
            counts = emptyList(),
        )
        state.applied(appliedStartDate = state.startDate, appliedEndDate = appliedEndDate, lastBattleAt = state.lastBattleAt)
    }

    /** 그날의 전투 중 id 가 체크포인트 이하인 것을 새 집계기에 모은다. */
    private fun <TBattle : BaseStatsBattleEntity> readDay(
        aggregation: PatchStatsAggregation<TBattle>,
        state: AlignState,
        day: LocalDate,
    ): DayRead {
        val aggregator = aggregation.createAggregator()
        var lastBattleAt: Instant? = null
        aggregation.reader.read(
            dateRange = PatchDateRange(day, day),
            afterBattleId = 0,
            battleIdCap = state.lastReadBattleId,
        ) { battles ->
            battles.forEach { aggregator.aggregate(it) }
            lastBattleAt = listOfNotNull(lastBattleAt, battles.maxOf { it.battleAt }).max()
        }
        return DayRead(counts = aggregator.toCounts(), lastBattleAt = lastBattleAt)
    }

    private class DayRead(
        val counts: List<PatchStatsCount>,
        val lastBattleAt: Instant?,
    )

    /** 맞추는 동안 바뀌는 체크포인트 줄의 값. 쓰기가 성공할 때마다 [applied] 로 옮기고 수정 번호를 1 올린다. */
    private class AlignState(
        checkpoint: StatsCheckpointEntity,
    ) {
        val checkpointId: Long = checkNotNull(checkpoint.id)
        val brawlStarsPatchId: Long = checkpoint.brawlStarsPatchId
        val lastReadBattleId: Long = checkpoint.lastReadBattleId
        var version: Long = checkpoint.version
            private set
        var startDate: LocalDate = checkpoint.appliedStartDate
            private set
        var endDate: LocalDate? = checkpoint.appliedEndDate
            private set
        var lastBattleAt: Instant? = checkpoint.lastBattleAt
            private set

        /** 적용한 범위의 마지막 날. 끝이 없으면 오늘이다. */
        fun lastDay(today: LocalDate): LocalDate = endDate ?: today

        /** 적용한 범위에 하루만 남았다. */
        fun isLastDay(today: LocalDate): Boolean = startDate == lastDay(today)

        fun update(
            appliedStartDate: LocalDate,
            appliedEndDate: LocalDate?,
            lastBattleAt: Instant?,
        ) = StatsCheckpointUpdate(
            checkpointId = checkpointId,
            version = version,
            lastReadBattleId = lastReadBattleId,
            appliedStartDate = appliedStartDate,
            appliedEndDate = appliedEndDate,
            lastBattleAt = lastBattleAt,
        )

        fun applied(
            appliedStartDate: LocalDate,
            appliedEndDate: LocalDate?,
            lastBattleAt: Instant?,
        ) {
            startDate = appliedStartDate
            endDate = appliedEndDate
            this.lastBattleAt = lastBattleAt
            version++
        }
    }
}
