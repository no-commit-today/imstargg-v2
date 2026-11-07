package com.imstargg.core.batch.job.statistics.pairbattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BrawlerPairBattleResultStatsTask(
    private val reader: BrawlerPairBattleResultStatsReader,
    private val writer: BrawlerPairBattleResultStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info { "[$JOB_NAME] 주간 범위 ${weekRanges.size}개 처리 시작" }

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val aggregator =
            BrawlerPairBattleResultStatsAggregator(
                startBattleDate = weekRange.start,
                endBattleDate = weekRange.end,
            )
        val totalRead =
            reader.read(weekRange) { battles ->
                battles.forEach { aggregator.aggregate(it) }
            }
        val totalEntities = writer.write(weekRange, aggregator)

        log.info {
            "[$JOB_NAME] 주간 범위 완료 - weekRange: ${weekRange.start} ~ ${weekRange.end}, 전투: ${totalRead}건, 집계: ${totalEntities}건"
        }
    }

    companion object {
        private const val JOB_NAME = "brawlerPairBattleResultStatsJob"
    }
}
