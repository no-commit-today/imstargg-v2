package com.imstargg.core.batch.job.statistics.triobattleresult

import com.imstargg.core.batch.domain.statistics.BrawlerTrioBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BrawlerTrioBattleResultStatsTask(
    private val reader: BrawlerTrioBattleResultStatsReader,
    private val writer: BrawlerTrioBattleResultStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info { "[$JOB_NAME] 주간 범위 ${weekRanges.size}개 처리 시작" }

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val aggregator =
            BrawlerTrioBattleResultStatsAggregator(
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
        private const val JOB_NAME = "brawlerTrioBattleResultStatsJob"
    }
}
