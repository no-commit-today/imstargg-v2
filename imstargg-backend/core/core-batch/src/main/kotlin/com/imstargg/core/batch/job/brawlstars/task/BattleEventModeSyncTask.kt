package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.EventTypeResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BattleEventModeSyncTask(
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val alertManager: AlertManager,
) {
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val gameModes = brawlStarsClient.getGameModes().items
        val existingIds =
            battleEventModeJpaRepository
                .findAll()
                .map { it.brawlStarsId }
                .toSet()

        val newModes = gameModes.filter { it.id !in existingIds }
        alertNewModes(newModes)
        log.info("[{}] 완료 - 신규 모드: {}건", JOB_NAME, newModes.size)
    }

    private fun alertNewModes(newModes: List<EventTypeResponse>) {
        if (newModes.isEmpty()) return
        val content = newModes.joinToString("\n") { it.toString() }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 새로운 이벤트 모드 발견")
                .content(content)
                .build(),
        )
    }

    companion object {
        private const val JOB_NAME = "battleEventModeSyncJob"
        private val log = LoggerFactory.getLogger(BattleEventModeSyncTask::class.java)
    }
}
