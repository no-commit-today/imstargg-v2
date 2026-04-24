package com.imstargg.core.batch.job.statistics

import com.imstargg.core.batch.domain.BrawlerTeamBattleRankStatsAggregator
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.StatsWeekRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.querydsl.jpa.JPAExpressions
import jakarta.persistence.EntityManager
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerTeamBattleRankStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val em: EntityManager,
) {
    companion object {
        const val JOB_NAME = "brawlerTeamBattleRankStatsJob"
        const val DATE_PARAMETER_NAME = "brawlerTeamBattleRankStatsJobDateJobParameter"
        private const val STEP_NAME = "brawlerTeamBattleRankStatsStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun brawlerTeamBattleRankStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerTeamBattleRankStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerTeamBattleRankStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                PerEventWeeklyBattleStatsTasklet(
                    em = em,
                    jobName = JOB_NAME,
                    weekRangesSupplier = { StatsWeekRange.currentAndPrevious(dateJobParameter().date!!) },
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
                        BrawlerTeamBattleRankStatsAggregator(
                            startBattleDate = weekRange.start,
                            endBattleDate = weekRange.end,
                        )
                    },
                    deleteExisting = { queryFactory, weekRange, eventBrawlStarsId ->
                        queryFactory
                            .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                            .where(
                                brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                                    JPAExpressions
                                        .select(brawlerTeamBattleRankStatsEntity.id)
                                        .from(brawlerTeamBattleRankStatsEntity)
                                        .where(
                                            brawlerTeamBattleRankStatsEntity.startBattleDate
                                                .eq(weekRange.start),
                                            brawlerTeamBattleRankStatsEntity.endBattleDate
                                                .eq(weekRange.end),
                                            brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                                .eq(eventBrawlStarsId),
                                        ),
                                ),
                            ).execute()
                        queryFactory
                            .delete(brawlerTeamBattleRankStatsEntity)
                            .where(
                                brawlerTeamBattleRankStatsEntity.startBattleDate.eq(weekRange.start),
                                brawlerTeamBattleRankStatsEntity.endBattleDate.eq(weekRange.end),
                                brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                    .eq(eventBrawlStarsId),
                            ).execute()
                    },
                    persistAdditional = { em, parent ->
                        parent.brawlerTeam.brawlerBrawlStarsIds.forEach { brawlerId ->
                            em.persist(
                                BrawlerTeamBattleRankStatsBrawlerEntity(
                                    stats = parent,
                                    brawlerBrawlStarsId = brawlerId,
                                ),
                            )
                        }
                    },
                ),
                txManager,
            ).build()
}
