package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationJpaRepository
import org.slf4j.LoggerFactory
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
    private val battleEventRotationJpaRepository: BattleEventRotationJpaRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "battleEventRotationSyncJob"
        const val STEP_NAME = "battleEventRotationSyncStep"
        private val log = LoggerFactory.getLogger(BattleEventRotationSyncJobConfig::class.java)
    }

    @Bean(JOB_NAME)
    fun battleEventRotationSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(battleEventRotationSyncStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun battleEventRotationSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    log.info("[{}] 시작", JOB_NAME)
                    val now = OffsetDateTime.now(clock)
                    val rotations = brawlStarsClient.getEventRotation()

                    val rotation =
                        battleEventRotationJpaRepository.save(
                            BattleEventRotationEntity(collectedAt = now),
                        )

                    val items =
                        rotations.map {
                            BattleEventRotationItemEntity(
                                battleEventRotationId = rotation.id!!,
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
                    battleEventRotationItemJpaRepository.saveAll(items)
                    log.info("[{}] 완료 - 로테이션 슬롯 {}건 저장", JOB_NAME, items.size)
                },
                txManager,
            ).build()
}
