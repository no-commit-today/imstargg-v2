package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamEmbedded
import com.imstargg.infrastructure.db.core.statistics.QBrawlerEnemyBattleResultStatsEntity.Companion.brawlerEnemyBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsBrawlerEntity.Companion.brawlerTeamBattleRankStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleRankStatsEntity.Companion.brawlerTeamBattleRankStatsEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsBrawlerEntity.Companion.brawlerTeamBattleResultStatsBrawlerEntity
import com.imstargg.infrastructure.db.core.statistics.QBrawlerTeamBattleResultStatsEntity.Companion.brawlerTeamBattleResultStatsEntity
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

private data class TeamGroupKey(
    val tierRange: StatsTierRange,
    val brawlerBrawlStarsIds: List<Long>,
)

private data class EnemyGroupKey(
    val tierRange: StatsTierRange,
    val brawlerBrawlStarsId: Long,
    val enemyBrawlerBrawlStarsId: Long,
)

@Component
class PeriodStatsAggregationHelper(
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun aggregateTeamResult(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        aggregate(
            sourcePeriodType = sourcePeriodType,
            targetPeriodType = targetPeriodType,
            startDate = startDate,
            endDate = endDate,
            jobName = jobName,
            label = "Team Result",
            queryEventIds = { qf ->
                qf
                    .select(brawlerTeamBattleResultStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerTeamBattleResultStatsEntity)
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                    ).fetch()
                    .toList()
            },
            querySourceEntities = { qf, eventId ->
                qf
                    .selectFrom(brawlerTeamBattleResultStatsEntity)
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                        brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).fetch()
                    .toList()
            },
            groupBy = { TeamGroupKey(it.tierRange, it.brawlerTeam.brawlerBrawlStarsIds) },
            buildTarget = { eventId, key, group ->
                BrawlerTeamBattleResultStatsEntity(
                    eventBrawlStarsId = eventId,
                    periodType = targetPeriodType,
                    tierRange = key.tierRange,
                    battleDate = startDate,
                    resultCount = group.map { it.resultCount }.reduce { acc, r -> acc + r },
                    brawlerTeam = BrawlerTeamEmbedded.of(key.brawlerBrawlStarsIds),
                )
            },
            persistAdditional = { em, parent ->
                parent.brawlerTeam.brawlerBrawlStarsIds.forEach { brawlerId ->
                    em.persist(
                        BrawlerTeamBattleResultStatsBrawlerEntity(
                            stats = parent,
                            brawlerBrawlStarsId = brawlerId,
                        ),
                    )
                }
            },
            deleteExistingTarget = { qf, eventId ->
                qf
                    .delete(brawlerTeamBattleResultStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleResultStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleResultStatsEntity.id)
                                .from(brawlerTeamBattleResultStatsEntity)
                                .where(
                                    brawlerTeamBattleResultStatsEntity.periodType
                                        .eq(targetPeriodType),
                                    brawlerTeamBattleResultStatsEntity.battleDate.eq(startDate),
                                    brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                qf
                    .delete(brawlerTeamBattleResultStatsEntity)
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(targetPeriodType),
                        brawlerTeamBattleResultStatsEntity.battleDate.eq(startDate),
                        brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
            deleteSource = { qf, eventId ->
                qf
                    .delete(brawlerTeamBattleResultStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleResultStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleResultStatsEntity.id)
                                .from(brawlerTeamBattleResultStatsEntity)
                                .where(
                                    brawlerTeamBattleResultStatsEntity.periodType
                                        .eq(sourcePeriodType),
                                    brawlerTeamBattleResultStatsEntity.battleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleResultStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                qf
                    .delete(brawlerTeamBattleResultStatsEntity)
                    .where(
                        brawlerTeamBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleResultStatsEntity.battleDate.between(startDate, endDate),
                        brawlerTeamBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
        )
    }

    fun aggregateTeamRank(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        aggregate(
            sourcePeriodType = sourcePeriodType,
            targetPeriodType = targetPeriodType,
            startDate = startDate,
            endDate = endDate,
            jobName = jobName,
            label = "Team Rank",
            queryEventIds = { qf ->
                qf
                    .select(brawlerTeamBattleRankStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerTeamBattleRankStatsEntity)
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleRankStatsEntity.battleDate.between(startDate, endDate),
                    ).fetch()
                    .toList()
            },
            querySourceEntities = { qf, eventId ->
                qf
                    .selectFrom(brawlerTeamBattleRankStatsEntity)
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleRankStatsEntity.battleDate.between(startDate, endDate),
                        brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).fetch()
                    .toList()
            },
            groupBy = { TeamGroupKey(it.tierRange, it.brawlerTeam.brawlerBrawlStarsIds) },
            buildTarget = { eventId, key, group ->
                BrawlerTeamBattleRankStatsEntity(
                    eventBrawlStarsId = eventId,
                    periodType = targetPeriodType,
                    tierRange = key.tierRange,
                    battleDate = startDate,
                    rankCounts = group.map { it.rankCounts }.reduce { acc, r -> acc + r },
                    brawlerTeam = BrawlerTeamEmbedded.of(key.brawlerBrawlStarsIds),
                )
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
            deleteExistingTarget = { qf, eventId ->
                qf
                    .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleRankStatsEntity.id)
                                .from(brawlerTeamBattleRankStatsEntity)
                                .where(
                                    brawlerTeamBattleRankStatsEntity.periodType
                                        .eq(targetPeriodType),
                                    brawlerTeamBattleRankStatsEntity.battleDate.eq(startDate),
                                    brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                qf
                    .delete(brawlerTeamBattleRankStatsEntity)
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(targetPeriodType),
                        brawlerTeamBattleRankStatsEntity.battleDate.eq(startDate),
                        brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
            deleteSource = { qf, eventId ->
                qf
                    .delete(brawlerTeamBattleRankStatsBrawlerEntity)
                    .where(
                        brawlerTeamBattleRankStatsBrawlerEntity.stats.id.`in`(
                            JPAExpressions
                                .select(brawlerTeamBattleRankStatsEntity.id)
                                .from(brawlerTeamBattleRankStatsEntity)
                                .where(
                                    brawlerTeamBattleRankStatsEntity.periodType
                                        .eq(sourcePeriodType),
                                    brawlerTeamBattleRankStatsEntity.battleDate
                                        .between(startDate, endDate),
                                    brawlerTeamBattleRankStatsEntity.eventBrawlStarsId
                                        .eq(eventId),
                                ),
                        ),
                    ).execute()
                qf
                    .delete(brawlerTeamBattleRankStatsEntity)
                    .where(
                        brawlerTeamBattleRankStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerTeamBattleRankStatsEntity.battleDate.between(startDate, endDate),
                        brawlerTeamBattleRankStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
        )
    }

    fun aggregateEnemyResult(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
    ) {
        aggregate(
            sourcePeriodType = sourcePeriodType,
            targetPeriodType = targetPeriodType,
            startDate = startDate,
            endDate = endDate,
            jobName = jobName,
            label = "Enemy Result",
            queryEventIds = { qf ->
                qf
                    .select(brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId)
                    .distinct()
                    .from(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerEnemyBattleResultStatsEntity.battleDate
                            .between(startDate, endDate),
                    ).fetch()
                    .toList()
            },
            querySourceEntities = { qf, eventId ->
                qf
                    .selectFrom(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerEnemyBattleResultStatsEntity.battleDate
                            .between(startDate, endDate),
                        brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).fetch()
                    .toList()
            },
            groupBy = {
                EnemyGroupKey(
                    tierRange = it.tierRange,
                    brawlerBrawlStarsId = it.brawlerBrawlStarsId,
                    enemyBrawlerBrawlStarsId = it.enemyBrawlerBrawlStarsId,
                )
            },
            buildTarget = { eventId, key, group ->
                BrawlerEnemyBattleResultStatsEntity(
                    eventBrawlStarsId = eventId,
                    periodType = targetPeriodType,
                    tierRange = key.tierRange,
                    battleDate = startDate,
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    enemyBrawlerBrawlStarsId = key.enemyBrawlerBrawlStarsId,
                    resultCount = group.map { it.resultCount }.reduce { acc, r -> acc + r },
                )
            },
            deleteExistingTarget = { qf, eventId ->
                qf
                    .delete(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(targetPeriodType),
                        brawlerEnemyBattleResultStatsEntity.battleDate.eq(startDate),
                        brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
            deleteSource = { qf, eventId ->
                qf
                    .delete(brawlerEnemyBattleResultStatsEntity)
                    .where(
                        brawlerEnemyBattleResultStatsEntity.periodType.eq(sourcePeriodType),
                        brawlerEnemyBattleResultStatsEntity.battleDate
                            .between(startDate, endDate),
                        brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(eventId),
                    ).execute()
            },
        )
    }

    private fun <TSource, TKey, TTarget> aggregate(
        sourcePeriodType: StatsPeriodType,
        targetPeriodType: StatsPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        jobName: String,
        label: String,
        queryEventIds: (JPAQueryFactory) -> List<Long>,
        querySourceEntities: (JPAQueryFactory, Long) -> List<TSource>,
        groupBy: (TSource) -> TKey,
        buildTarget: (Long, TKey, List<TSource>) -> TTarget,
        deleteExistingTarget: (JPAQueryFactory, Long) -> Unit,
        deleteSource: (JPAQueryFactory, Long) -> Unit,
        persistAdditional: (EntityManager, TTarget) -> Unit = { _, _ -> },
    ) {
        entityManagerFactory.createEntityManager().use { entityManager ->
            val queryFactory = JPAQueryFactory(entityManager)
            val eventIds = queryEventIds(queryFactory)

            var totalAggregated = 0L
            var totalDeleted = 0L

            for (eventId in eventIds) {
                entityManager.transaction.begin()
                deleteExistingTarget(queryFactory, eventId)
                entityManager.transaction.commit()

                val sourceEntities = querySourceEntities(queryFactory, eventId)
                val targetEntities =
                    sourceEntities.groupBy(groupBy).map { (key, group) ->
                        buildTarget(eventId, key, group)
                    }

                targetEntities.chunked(BattleStatsConstants.BATCH_SIZE).forEach { batch ->
                    entityManager.transaction.begin()
                    batch.forEach {
                        entityManager.persist(it)
                        persistAdditional(entityManager, it)
                    }
                    entityManager.transaction.commit()
                    entityManager.clear()
                }
                totalAggregated += targetEntities.size

                entityManager.transaction.begin()
                deleteSource(queryFactory, eventId)
                entityManager.transaction.commit()
                totalDeleted += sourceEntities.size
            }

            log.info(
                "[{}] {} 집계 완료 - 기간: {} ~ {}, 집계: {}건, 삭제: {}건",
                jobName,
                label,
                startDate,
                endDate,
                totalAggregated,
                totalDeleted,
            )
        }
    }
}
