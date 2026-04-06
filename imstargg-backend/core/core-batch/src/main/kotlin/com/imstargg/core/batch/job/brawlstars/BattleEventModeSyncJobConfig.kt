package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.EventTypeResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BattleEventModeSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val alertManager: AlertManager,
) {
    companion object {
        const val JOB_NAME = "battleEventModeSyncJob"
        const val STEP_NAME = "battleEventModeSyncStep"
    }

    @Bean(JOB_NAME)
    fun battleEventModeSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(battleEventModeSyncStep())
            .build()

    @Bean(STEP_NAME)
    fun battleEventModeSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    val gameModes = brawlStarsClient.getGameModes().items
                    val existingIds =
                        battleEventModeJpaRepository
                            .findAll()
                            .map { it.brawlStarsId }
                            .toSet()

                    val newModes = gameModes.filter { it.id !in existingIds }
                    alertNewModes(newModes)
                },
                txManager,
            ).build()

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
}
