package com.imstargg.core.batch.job.statistics.pairbattlerank

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.enums.WeekRange
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BrawlerPairBattleRankStatsTask(
    private val reader: BrawlerPairBattleRankStatsReader,
    private val writer: BrawlerPairBattleRankStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info { "[$JOB_NAME] 주간 범위 ${weekRanges.size}개 처리 시작" }
        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        log.info { "[$JOB_NAME] 이벤트 ${eventIds.size}개 처리 시작 - weekRange: ${weekRange.start} ~ ${weekRange.end}" }

        var totalRead = 0L
        var totalEntities = 0L
        for (eventId in eventIds) {
            val aggregator =
                BrawlerPairBattleRankStatsAggregator(
                    startBattleDate = weekRange.start,
                    endBattleDate = weekRange.end,
                )
            totalRead +=
                reader.read(weekRange, eventId) { battles ->
                    battles.forEach { aggregator.aggregate(it) }
                }
            totalEntities += writer.write(weekRange, eventId, aggregator)
        }

        log.info {
            "[$JOB_NAME] 주간 범위 완료 - weekRange: ${weekRange.start} ~ ${weekRange.end}, 전투: ${totalRead}건, 집계: ${totalEntities}건"
        }
    }

    companion object {
        private const val JOB_NAME = "brawlerPairBattleRankStatsJob"
    }
}
