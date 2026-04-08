package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PeriodStatsAggregationHelper(
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 1000
    }

    fun aggregateTeamResult(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { entityManager ->
            val queryFactory = JPAQueryFactory(entityManager)

            val eventIds =
                queryFactory
                    .select(brawlerTeamBattleResultStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerTeamBattleResultStatsEntity)
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleResultStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            var totalAggregated = 0L
            var totalDeleted = 0L

            for (eventId in eventIds) {
                val sourceEntities =
                    queryFactory
                        .selectFrom(brawlerTeamBattleResultStatsEntity)
                        .distinct()
                        .leftJoin(
                            brawlerTeamBattleResultStatsEntity.brawlers,
                            brawlerTeamBattleResultStatsBrawlerEntity,
                        ).fetchJoin()
                        .where(
                            brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerTeamBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).fetch()
                        .toList()

                val aggregated =
                    sourceEntities.groupBy {
                        Pair(it.tierRange, it.brawlerBrawlStarsIds)
                    }

                val targetEntities =
                    aggregated.map { (key, group) ->
                        val totalResult = group.map { it.resultCount }.reduce { acc, r -> acc + r }
                        BrawlerTeamBattleResultStatsEntity(
                            eventBrawlStarsId = eventId,
                            periodType = targetPeriodType,
                            tierRange = key.first,
                            startBattleDate = startDate,
                            endBattleDate = endDate,
                            teamSize = StatsTeamSize.fromValue(key.second.size),
                            resultCount = totalResult,
                            initialBrawlerBrawlStarsIds = key.second,
                        )
                    }

                targetEntities.chunked(BATCH_SIZE).forEach { batch ->
                    entityManager.transaction.begin()
                    batch.forEach { entityManager.persist(it) }
                    entityManager.transaction.commit()
                    entityManager.clear()
                }
                totalAggregated += targetEntities.size

                entityManager.transaction.begin()
                queryFactory
                    .delete(brawlerTeamBattleResultStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleResultStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleResultStatsEntity.id)
                                .from(brawlerTeamBattleResultStatsEntity)
                                .where(
                                    brawlerTeamBattleResultStatsEntity.periodType
                                        .eq(sourcePeriodType),
                                    brawlerTeamBattleResultStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                val deleted =
                    queryFactory
                        .delete(brawlerTeamBattleResultStatsEntity)
                        .where(
                            brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerTeamBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).execute()
                entityManager.transaction.commit()
                totalDeleted += deleted
            }

            log.info(
                "[{}] Team Result 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                jobName,
                startDate,
                endDate,
                totalAggregated,
                totalDeleted,
            )
        }
    }

    fun aggregateTeamRank(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { entityManager ->
            val queryFactory = JPAQueryFactory(entityManager)

            val eventIds =
                queryFactory
                    .select(brawlerTeamBattleRankStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerTeamBattleRankStatsEntity)
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleRankStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            var totalAggregated = 0L
            var totalDeleted = 0L

            for (eventId in eventIds) {
                val sourceEntities =
                    queryFactory
                        .selectFrom(brawlerTeamBattleRankStatsEntity)
                        .distinct()
                        .leftJoin(
                            brawlerTeamBattleRankStatsEntity.brawlers,
                            brawlerTeamBattleRankStatsBrawlerEntity,
                        ).fetchJoin()
                        .where(
                            brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerTeamBattleRankStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).fetch()
                        .toList()

                val aggregated =
                    sourceEntities.groupBy {
                        Pair(it.tierRange, it.brawlerBrawlStarsIds)
                    }

                val targetEntities =
                    aggregated.map { (key, group) ->
                        val combinedRanks =
                            group.map { it.rankCounts }.reduce { acc, r -> acc + r }
                        BrawlerTeamBattleRankStatsEntity(
                            eventBrawlStarsId = eventId,
                            periodType = targetPeriodType,
                            tierRange = key.first,
                            startBattleDate = startDate,
                            endBattleDate = endDate,
                            teamSize = StatsTeamSize.fromValue(key.second.size),
                            rankCounts = combinedRanks,
                            initialBrawlerBrawlStarsIds = key.second,
                        )
                    }

                targetEntities.chunked(BATCH_SIZE).forEach { batch ->
                    entityManager.transaction.begin()
                    batch.forEach { entityManager.persist(it) }
                    entityManager.transaction.commit()
                    entityManager.clear()
                }
                totalAggregated += targetEntities.size

                entityManager.transaction.begin()
                queryFactory
                    .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleRankStatsEntity.id)
                                .from(brawlerTeamBattleRankStatsEntity)
                                .where(
                                    brawlerTeamBattleRankStatsEntity.periodType
                                        .eq(sourcePeriodType),
                                    brawlerTeamBattleRankStatsEntity.startBattleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                val deleted =
                    queryFactory
                        .delete(brawlerTeamBattleRankStatsEntity)
                        .where(
                            brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerTeamBattleRankStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).execute()
                entityManager.transaction.commit()
                totalDeleted += deleted
            }

            log.info(
                "[{}] Team Rank 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                jobName,
                startDate,
                endDate,
                totalAggregated,
                totalDeleted,
            )
        }
    }

    fun aggregateEnemyResult(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        val entityManager = entityManagerFactory.createEntityManager()

        try {
            val queryFactory = JPAQueryFactory(entityManager)

            val eventIds =
                queryFactory
                    .select(brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerEnemyBattleResultStatsEntity.startBattleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()

            var totalAggregated = 0L
            var totalDeleted = 0L

            for (eventId in eventIds) {
                val sourceEntities =
                    queryFactory
                        .selectFrom(brawlerEnemyBattleResultStatsEntity)
                        .where(
                            brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerEnemyBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).fetch()
                        .toList()

                val aggregated =
                    sourceEntities.groupBy {
                        Triple(
                            it.tierRange,
                            it.brawlerBrawlStarsId,
                            it.enemyBrawlerBrawlStarsId,
                        )
                    }

                val targetEntities =
                    aggregated.map { (key, group) ->
                        val totalResult = group.map { it.resultCount }.reduce { acc, r -> acc + r }
                        BrawlerEnemyBattleResultStatsEntity(
                            eventBrawlStarsId = eventId,
                            periodType = targetPeriodType,
                            tierRange = key.first,
                            startBattleDate = startDate,
                            endBattleDate = endDate,
                            brawlerBrawlStarsId = key.second,
                            enemyBrawlerBrawlStarsId = key.third,
                            resultCount = totalResult,
                        )
                    }

                targetEntities.chunked(BATCH_SIZE).forEach { batch ->
                    entityManager.transaction.begin()
                    batch.forEach { entityManager.persist(it) }
                    entityManager.transaction.commit()
                    entityManager.clear()
                }
                totalAggregated += targetEntities.size

                entityManager.transaction.begin()
                val deleted =
                    queryFactory
                        .delete(brawlerEnemyBattleResultStatsEntity)
                        .where(
                            brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                            brawlerEnemyBattleResultStatsEntity.startBattleDate
                                .between(startDate, endDate),
                            brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                        ).execute()
                entityManager.transaction.commit()
                totalDeleted += deleted
            }

            log.info(
                "[{}] Enemy Result 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                jobName,
                startDate,
                endDate,
                totalAggregated,
                totalDeleted,
            )
        } finally {
            entityManager.close()
        }
    }
}
