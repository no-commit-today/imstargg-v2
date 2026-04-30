package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.job.statistics.task.StatsPlayerSyncFromRankingTask
import com.imstargg.core.batch.job.statistics.writer.StatsPlayerSyncJobItemWriter
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.batch.support.querydsl.QuerydslPagingItemReader
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
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
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class StatsPlayerSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val statsPlayerSyncFromRankingTask: StatsPlayerSyncFromRankingTask,
) {
    companion object {
        const val JOB_NAME = "statsPlayerSyncJob"
        const val RANKING_JOB_NAME = "statsPlayerSyncFromRankingJob"
        const val DATE_PARAMETER_NAME = "statsPlayerSyncJobDateJobParameter"
        const val RANKING_STEP_NAME = "statsPlayerSyncFromRankingStep"
        const val RANK_BATTLE_STEP_NAME = "statsPlayerSyncFromRankBattleStep"
        const val RESULT_BATTLE_STEP_NAME = "statsPlayerSyncFromResultBattleStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun statsPlayerSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(statsPlayerSyncFromRankBattleStep())
            .next(statsPlayerSyncFromResultBattleStep())
            .build()

    @Bean(RANKING_JOB_NAME)
    fun statsPlayerSyncFromRankingJob(): Job =
        JobBuilder(RANKING_JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(statsPlayerSyncFromRankingStep())
            .build()

    @Bean(RANKING_STEP_NAME)
    fun statsPlayerSyncFromRankingStep(): Step =
        StepBuilder(RANKING_STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                statsPlayerSyncFromRankingTask.sync()
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    @Bean(RANK_BATTLE_STEP_NAME)
    @JobScope
    fun statsPlayerSyncFromRankBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsRankBattleEntity, List<StatsBattleEntityTeamPlayer>>(
            RANK_BATTLE_STEP_NAME,
            jobRepository,
            CHUNK_SIZE,
        ).transactionManager(txManager)
            .reader(statsRankBattleReader())
            .processor(teamPlayerProcessor())
            .writer(statsPlayerSyncJobItemWriter())
            .build()

    @Bean(RESULT_BATTLE_STEP_NAME)
    @JobScope
    fun statsPlayerSyncFromResultBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsResultBattleEntity, List<StatsBattleEntityTeamPlayer>>(
            RESULT_BATTLE_STEP_NAME,
            jobRepository,
            CHUNK_SIZE,
        ).transactionManager(txManager)
            .reader(statsResultBattleReader())
            .processor(teamPlayerProcessor())
            .writer(statsPlayerSyncJobItemWriter())
            .build()

    @Bean
    @StepScope
    fun statsRankBattleReader(): QuerydslPagingItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.battleDate.eq(dateJobParameter().date))
                .orderBy(rankBattle.id.asc())
        }
    }

    @Bean
    @StepScope
    fun statsResultBattleReader(): QuerydslPagingItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.battleDate.eq(dateJobParameter().date))
                .orderBy(resultBattle.id.asc())
        }
    }

    @Bean
    @StepScope
    fun <T : BaseStatsBattleEntity> teamPlayerProcessor() =
        ItemProcessor<T, List<StatsBattleEntityTeamPlayer>> { battle ->
            battle.teams.flatten()
        }

    @Bean
    @StepScope
    fun statsPlayerSyncJobItemWriter() = StatsPlayerSyncJobItemWriter(statsPlayerJpaRepository)
}
