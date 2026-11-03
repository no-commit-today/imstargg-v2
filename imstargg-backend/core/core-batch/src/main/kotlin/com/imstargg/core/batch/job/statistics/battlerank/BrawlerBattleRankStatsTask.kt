package com.imstargg.core.batch.job.statistics.battlerank

import com.imstargg.core.batch.domain.statistics.BrawlerBattleRankStatsAggregator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Component
class BrawlerBattleRankStatsTask(
    private val reader: BrawlerBattleRankStatsReader,
    private val writer: BrawlerBattleRankStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info { "[$JOB_NAME] 시작 - date: $date" }
        val aggregator = BrawlerBattleRankStatsAggregator()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach { aggregator.aggregate(it) }
                log.debug { "[$JOB_NAME] 읽기 진행 - date: $date" }
            }
        val totalEntities = writer.write(date, aggregator)
        log.info { "[$JOB_NAME] 완료 - date: $date, 전투: ${totalRead}건, 집계: ${totalEntities}건" }
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleRankStatsJob"
    }
}
