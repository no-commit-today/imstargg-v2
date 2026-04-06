package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.OffsetDateTime

@Configuration
class BattleEventRotationSyncJobConfig(
    private val clock: Clock,
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "battleEventRotationSyncJob"
        const val STEP_NAME = "battleEventRotationSyncStep"
    }

    @Bean(JOB_NAME)
    fun battleEventRotationSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(battleEventRotationSyncStep())
            .build()

    @Bean(STEP_NAME)
    fun battleEventRotationSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    val now = OffsetDateTime.now(clock)
                    val rotations = brawlStarsClient.getEventRotation()
                    val activeSlotIds =
                        battleEventRotationItemJpaRepository
                            .findAllByEndTimeAfter(now)
                            .map { it.slotId }
                            .toSet()

                    val newRotations =
                        rotations
                            .filter { it.slotId !in activeSlotIds }
                            .map {
                                BattleEventRotationItemEntity(
                                    eventBrawlStarsId = it.event.id,
                                    eventModeName = it.event.mode,
                                    eventModeBrawlStarsId = it.event.modeId.toLong(),
                                    eventMapName = it.event.map,
                                    modifiers = it.event.modifiers ?: emptyList(),
                                    slotId = it.slotId,
                                    startTime = it.startTime,
                                    endTime = it.endTime,
                                )
                            }

                    battleEventRotationItemJpaRepository.saveAll(newRotations)
                },
                txManager,
            ).build()
}
