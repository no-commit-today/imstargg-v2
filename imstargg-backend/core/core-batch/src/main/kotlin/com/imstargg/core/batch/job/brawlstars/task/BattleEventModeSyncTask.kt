package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.EventTypeResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BattleEventModeSyncTask(
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val alertManager: AlertManager,
) {
    fun sync() {
        log.info { "[$JOB_NAME] 시작" }
        val gameModes = brawlStarsClient.getGameModes().items
        val existingIds =
            battleEventModeJpaRepository
                .findAll()
                .map { it.brawlStarsId }
                .toSet()

        val newModes = gameModes.filter { it.id !in existingIds }
        alertNewModes(newModes)
        log.info { "[$JOB_NAME] 완료 - 신규 모드: ${newModes.size}건" }
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
    }
}
