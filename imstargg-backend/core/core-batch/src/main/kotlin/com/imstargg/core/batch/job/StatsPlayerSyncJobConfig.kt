package com.imstargg.core.batch.job

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.batch.support.querydsl.QuerydslPagingItemReader
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerJpaRepository
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
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemProcessor
import org.springframework.batch.infrastructure.item.ItemWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class StatsPlayerSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
) {
    companion object {
        const val JOB_NAME = "statsPlayerSyncJob"
        const val DATE_PARAMETER_NAME = "statsPlayerSyncJobDateJobParameter"
        const val STEP_1_NAME = "statsPlayerSyncFromRankBattleStep"
        const val STEP_2_NAME = "statsPlayerSyncFromResultBattleStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun statsPlayerSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(statsPlayerSyncFromRankBattleStep())
            .next(statsPlayerSyncFromResultBattleStep())
            .build()

    @Bean(STEP_1_NAME)
    @JobScope
    fun statsPlayerSyncFromRankBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsRankBattleEntity, List<StatsBattleEntityTeamPlayer>>(STEP_1_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(statsRankBattleReader())
            .processor(teamPlayerProcessor())
            .writer(statsPlayerSyncJobItemWriter())
            .build()

    @Bean(STEP_2_NAME)
    @JobScope
    fun statsPlayerSyncFromResultBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsResultBattleEntity, List<StatsBattleEntityTeamPlayer>>(STEP_2_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
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
                .where(rankBattle.createdDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun statsResultBattleReader(): QuerydslPagingItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return QuerydslPagingItemReader(entityManagerFactory, CHUNK_SIZE) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.createdDate.eq(dateJobParameter().date))
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

    open class StatsPlayerSyncJobItemWriter(
        private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    ) : ItemWriter<List<StatsBattleEntityTeamPlayer>> {
        override fun write(chunk: Chunk<out List<StatsBattleEntityTeamPlayer>>) {
            val allTags =
                chunk.items
                    .flatten()
                    .map { it.brawlStarsTag }
                    .distinct()

            if (allTags.isEmpty()) return

            val existingTags =
                statsPlayerJpaRepository
                    .findAllByBrawlStarsTagIn(allTags)
                    .map { it.brawlStarsTag }
                    .toSet()

            val newPlayers =
                allTags
                    .filter { it !in existingTags }
                    .map { StatsPlayerEntity(brawlStarsTag = it) }

            if (newPlayers.isNotEmpty()) {
                statsPlayerJpaRepository.saveAll(newPlayers)
            }
        }
    }
}
