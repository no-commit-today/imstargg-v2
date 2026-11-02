package com.imstargg.core.batch.job.statistics.battleresult

import com.imstargg.core.batch.domain.statistics.BrawlerBattleResultStatsAggregator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Component
class BrawlerBattleResultStatsTask(
    private val reader: BrawlerBattleResultStatsReader,
    private val writer: BrawlerBattleResultStatsWriter,
) {
    fun aggregate(date: LocalDate) {
        log.info { "[$JOB_NAME] 시작 - date: $date" }
        val aggregator = BrawlerBattleResultStatsAggregator()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach { aggregator.aggregate(it) }
                log.debug { "[$JOB_NAME] 읽기 진행 - date: $date" }
            }
        val totalEntities = writer.write(date, aggregator)
        log.info { "[$JOB_NAME] 완료 - date: $date, 전투: ${totalRead}건, 집계: ${totalEntities}건" }
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleResultStatsJob"
    }
}
