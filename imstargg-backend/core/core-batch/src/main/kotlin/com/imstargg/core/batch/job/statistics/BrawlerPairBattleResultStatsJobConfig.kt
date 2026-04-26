package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleResultStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleResultStatsEntity.Companion.brawlerPairBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import jakarta.persistence.EntityManager
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerPairBattleResultStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val em: EntityManager,
) {
    companion object {
        const val JOB_NAME = "brawlerPairBattleResultStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerPairBattleResultStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerPairBattleResultStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerPairBattleResultStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerPairBattleResultStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerPairBattleResultStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                PerEventWeeklyBattleStatsTasklet(
                    em = em,
                    jobName = JOB_NAME,
                    weekRangesSupplier = { StatsWeekRange.currentAndPrevious(dateJobParameter().date!!) },
                    readDistinctEventIds = { queryFactory, weekRange ->
                        queryFactory
                            .select(statsResultBattleEntity.eventBrawlStarsId)
                            .distinct()
                            .from(statsResultBattleEntity)
                            .where(statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                            .fetch()
                            .toList()
                    },
                    readPage = { queryFactory, weekRange, lastId, eventBrawlStarsId ->
                        queryFactory
                            .selectFrom(statsResultBattleEntity)
                            .where(
                                statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                                statsResultBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                                statsResultBattleEntity.id.gt(lastId),
                            ).orderBy(statsResultBattleEntity.id.asc())
                            .limit(BattleStatsConstants.READ_PAGE_SIZE)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregatorFactory = { weekRange ->
                        BrawlerPairBattleResultStatsAggregator(
                            startBattleDate = weekRange.start,
                            endBattleDate = weekRange.end,
                        )
                    },
                    deleteExisting = { queryFactory, weekRange, eventBrawlStarsId ->
                        queryFactory
                            .delete(brawlerPairBattleResultStatsEntity)
                            .where(
                                brawlerPairBattleResultStatsEntity.startBattleDate.eq(weekRange.start),
                                brawlerPairBattleResultStatsEntity.endBattleDate.eq(weekRange.end),
                                brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            ).execute()
                    },
                ),
                txManager,
            ).build()
}
