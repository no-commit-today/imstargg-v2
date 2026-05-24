package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerBattleRankStatsAggregator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsTask(
    private val reader: BrawlerBattleRankStatsReader,
    private val writer: BrawlerBattleRankStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info("[{}] 시작 - date: {}", JOB_NAME, date)
        val aggregator = BrawlerBattleRankStatsAggregator()
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
        private const val JOB_NAME = "brawlerBattleRankStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerBattleRankStatsTask::class.java)
    }
}
