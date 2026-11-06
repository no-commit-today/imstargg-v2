package com.imstargg.core.batch.job.statistics.battleresult

import com.imstargg.core.batch.domain.statistics.BrawlerBattleResultStatsAggregator
import com.imstargg.core.batch.domain.statistics.StatsBotBattleMonitor
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * 하루치 전투를 브롤러별로 집계한다. 집계와 함께 봇 전투 판정 규칙의 이상 여부를 감시해(`StatsBotBattleMonitor`)
 * 쓰기 뒤에 요약 로그를 남기고, 경고 지표가 있으면 알림을 실행당 한 번 보낸다.
 *
 * 감시 구간의 예외는 로그로만 남기고 집계 잡을 실패시키지 않는다.
 * `DiscordAlertManager` 는 전송 실패를 삼키지만 알림을 만드는 중의 예외는 스텝을 실패시키기 때문이다.
 */
@Component
class BrawlerBattleResultStatsTask(
    private val reader: BrawlerBattleResultStatsReader,
    private val writer: BrawlerBattleResultStatsWriter,
    private val alertManager: AlertManager,
) {
    fun aggregate(date: LocalDate) {
        log.info { "[$JOB_NAME] 시작 - date: $date" }
        val aggregator = BrawlerBattleResultStatsAggregator()
        val monitor = StatsBotBattleMonitor()
        val totalRead =
            reader.read(date) { battles ->
                battles.forEach {
                    monitor.record(it)
                    aggregator.aggregate(it)
                }
                log.debug { "[$JOB_NAME] 읽기 진행 - date: $date" }
            }
        val totalEntities = writer.write(date, aggregator)
        log.info { "[$JOB_NAME] 완료 - date: $date, 전투: ${totalRead}건, 집계: ${totalEntities}건" }
        reportBotMonitor(date, monitor)
    }

    private fun reportBotMonitor(
        date: LocalDate,
        monitor: StatsBotBattleMonitor,
    ) {
        try {
            log.info { "[$JOB_NAME] 봇 감시 - date: $date, ${monitor.summary()}" }
            val warnings = monitor.metrics().filter { it.isWarning }
            if (warnings.isNotEmpty()) {
                alertManager.alert(
                    AlertCommand
                        .builder()
                        .warn()
                        .title("[$JOB_NAME] 봇 전투 규칙 이상 감지")
                        .content("date: $date\n" + warnings.joinToString("\n") { it.describe() })
                        .build(),
                )
            }
        } catch (ex: Exception) {
            log.error(ex) { "[$JOB_NAME] 봇 감시 요약 실패 - date: $date" }
        }
    }

    companion object {
        private const val JOB_NAME = "brawlerBattleResultStatsJob"
    }
}
