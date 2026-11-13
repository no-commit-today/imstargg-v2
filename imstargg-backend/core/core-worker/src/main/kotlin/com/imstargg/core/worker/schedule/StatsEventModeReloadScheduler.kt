package com.imstargg.core.worker.schedule

import com.imstargg.core.worker.application.stats.StatsEventModeSnapshot
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(prefix = "app.schedule.stats-event-mode-reload", name = ["enabled"], havingValue = "true")
class StatsEventModeReloadScheduler(
    private val statsEventModeSnapshot: StatsEventModeSnapshot,
) {
    @Scheduled(
        fixedDelayString = "\${app.schedule.stats-event-mode-reload.interval}",
        initialDelayString = "\${app.schedule.stats-event-mode-reload.interval}",
    )
    fun reload() {
        try {
            statsEventModeSnapshot.reload()
        } catch (e: Exception) {
            log.warn(e) { "통계 수집 모드 다시 읽기 실패, 이전 목록을 유지한다" }
        }
    }
}
