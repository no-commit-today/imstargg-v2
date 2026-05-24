package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerEnemyBattleResultStatsAggregator
import com.imstargg.core.enums.WeekRange
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerEnemyBattleResultStatsTask(
    private val reader: BrawlerEnemyBattleResultStatsReader,
    private val writer: BrawlerEnemyBattleResultStatsWriter,
) {
    fun aggregate(weekRanges: List<WeekRange>) {
        log.info("[{}] 주간 범위 {}개 처리 시작", JOB_NAME, weekRanges.size)

        for (weekRange in weekRanges) {
            processWeekRange(weekRange)
        }
    }

    private fun processWeekRange(weekRange: WeekRange) {
        val eventIds = reader.readDistinctEventIds(weekRange)
        log.info(
            "[{}] 이벤트 {}개 처리 시작 - weekRange: {} ~ {}",
            JOB_NAME,
            eventIds.size,
            weekRange.start,
            weekRange.end,
        )

        var totalRead = 0L
        var totalEntities = 0L
        for (eventId in eventIds) {
            val aggregator =
                BrawlerEnemyBattleResultStatsAggregator(
                    startBattleDate = weekRange.start,
                    endBattleDate = weekRange.end,
                )
            totalRead +=
                reader.read(weekRange, eventId) { battles ->
                    battles.forEach { aggregator.aggregate(it) }
                }
            totalEntities += writer.write(weekRange, eventId, aggregator)
        }

        log.info(
            "[{}] 주간 범위 완료 - weekRange: {} ~ {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            weekRange.start,
            weekRange.end,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerEnemyBattleResultStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerEnemyBattleResultStatsTask::class.java)
    }
}
