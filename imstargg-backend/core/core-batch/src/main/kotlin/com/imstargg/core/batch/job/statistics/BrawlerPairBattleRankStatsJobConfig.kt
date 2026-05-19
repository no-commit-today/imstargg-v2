package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerPairBattleRankStatsAggregator
import com.imstargg.core.batch.job.statistics.task.WeeklyEventBattleStatsAggregationTask
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import com.imstargg.infrastructure.db.core.statistics.QBrawlerPairBattleRankStatsEntity.Companion.brawlerPairBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BrawlerPairBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val aggregationTask: WeeklyEventBattleStatsAggregationTask,
) {
    companion object {
        const val JOB_NAME = "brawlerPairBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerPairBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerPairBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerPairBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(brawlerPairBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerPairBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                aggregationTask.aggregate(
                    jobName = JOB_NAME,
                    weekRanges = StatsWeekRange.currentAndPrevious(dateJobParameter().date!!),
                    readDistinctEventIds = { queryFactory, weekRange ->
                        queryFactory
                            .select(statsRankBattleEntity.eventBrawlStarsId)
                            .distinct()
                            .from(statsRankBattleEntity)
                            .where(statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                            .fetch()
                            .toList()
                    },
                    readPage = { queryFactory, weekRange, lastId, eventBrawlStarsId ->
                        queryFactory
                            .selectFrom(statsRankBattleEntity)
                            .where(
                                statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                                statsRankBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                                statsRankBattleEntity.id.gt(lastId),
                            ).orderBy(statsRankBattleEntity.id.asc())
                            .limit(BattleStatsConstants.READ_PAGE_SIZE)
                            .fetch()
                            .toList()
                    },
                    getId = { it.id!! },
                    aggregatorFactory = { weekRange ->
                        BrawlerPairBattleRankStatsAggregator(
                            startBattleDate = weekRange.start,
                            endBattleDate = weekRange.end,
                        )
                    },
                    deleteExisting = { queryFactory, weekRange, eventBrawlStarsId ->
                        queryFactory
                            .delete(brawlerPairBattleRankStatsEntity)
                            .where(
                                brawlerPairBattleRankStatsEntity.startBattleDate.eq(weekRange.start),
                                brawlerPairBattleRankStatsEntity.endBattleDate.eq(weekRange.end),
                                brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            ).execute()
                    },
                )
                RepeatStatus.FINISHED
            })
            .build()
}
