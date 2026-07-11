package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.batch.support.querydsl.KeysetItemReader
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BattleEventSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) {
    companion object {
        const val JOB_NAME = "battleEventSyncJob"
        const val DATE_PARAMETER_NAME = "battleEventSyncJobDateJobParameter"
        const val STEP_1_NAME = "battleEventSyncFromRankBattleStep"
        const val STEP_2_NAME = "battleEventSyncFromResultBattleStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun battleEventSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(battleEventSyncFromRankBattleStep())
            .next(battleEventSyncFromResultBattleStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_1_NAME)
    @JobScope
    fun battleEventSyncFromRankBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsRankBattleEntity, StatsRankBattleEntity>(STEP_1_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncRankBattleReader())
            .writer(battleEventSyncFromRankBattleWriter())
            .build()

    @Bean(STEP_2_NAME)
    @JobScope
    fun battleEventSyncFromResultBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsResultBattleEntity, StatsResultBattleEntity>(STEP_2_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncResultBattleReader())
            .writer(battleEventSyncFromResultBattleWriter())
            .build()

    @Bean
    @StepScope
    fun battleEventSyncRankBattleReader(): KeysetItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncRankBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = rankBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncResultBattleReader(): KeysetItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncResultBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = resultBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncFromRankBattleWriter() =
        BattleEventSyncItemWriter<StatsRankBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )

    @Bean
    @StepScope
    fun battleEventSyncFromResultBattleWriter() =
        BattleEventSyncItemWriter<StatsResultBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )
}
