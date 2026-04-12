package com.imstargg.core.worker.schedule

import com.imstargg.core.worker.application.StatsBattleAccumulationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "app.schedule.stats-battle", name = ["enabled"], havingValue = "true")
class StatsBattleAccumulationScheduler(
    private val statsBattleAccumulationService: StatsBattleAccumulationService,
) {
    @Scheduled(fixedDelayString = "\${app.schedule.stats-battle.interval}")
    fun accumulate() {
        statsBattleAccumulationService.accumulate()
    }
}
