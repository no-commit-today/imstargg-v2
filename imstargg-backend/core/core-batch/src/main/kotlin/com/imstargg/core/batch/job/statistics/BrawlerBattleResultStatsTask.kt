package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleResultStatsAggregator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsTask(
    private val reader: BrawlerBattleResultStatsReader,
    private val writer: BrawlerBattleResultStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info("[{}] 시작 - date: {}", JOB_NAME, date)
        val aggregator = BrawlerBattleResultStatsAggregator()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach { aggregator.aggregate(it) }
                log.debug("[{}] 읽기 진행 - date: {}", JOB_NAME, date)
            }
        val totalEntities = writer.write(date, aggregator)
        log.info(
            "[{}] 완료 - date: {}, 전투: {}건, 집계: {}건",
            JOB_NAME,
            date,
            totalRead,
            totalEntities,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleResultStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerBattleResultStatsTask::class.java)
    }
}
