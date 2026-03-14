package com.imstargg.core.batch.job

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.OffsetDateTime

@Configuration
class EventRotationSyncJobConfig(
    private val clock: Clock,
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "eventRotationSyncJob"
        const val STEP_NAME = "eventRotationSyncStep"
    }

    @Bean(JOB_NAME)
    fun eventRotationSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(eventRotationSyncStep())
            .build()

    @Bean(STEP_NAME)
    fun eventRotationSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
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
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
