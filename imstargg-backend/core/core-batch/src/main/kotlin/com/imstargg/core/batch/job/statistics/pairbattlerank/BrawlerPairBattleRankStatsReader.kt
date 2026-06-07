package com.imstargg.core.batch.job.statistics.pairbattlerank

import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerPairBattleRankStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun readDistinctEventIds(weekRange: WeekRange): List<Long> {
        val entityManager = entityManagerFactory.createEntityManager()
        entityManager.use { em ->
            return JPAQueryFactory(em)
                .select(statsRankBattleEntity.eventBrawlStarsId)
                .distinct()
                .from(statsRankBattleEntity)
                .where(statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end))
                .fetch()
                .toList()
        }
    }

    fun read(
        weekRange: WeekRange,
        eventBrawlStarsId: Long,
        consume: (List<StatsRankBattleEntity>) -> Unit,
    ): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsRankBattleEntity)
                        .where(
                            statsRankBattleEntity.battleDate.between(weekRange.start, weekRange.end),
                            statsRankBattleEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
                            statsRankBattleEntity.id.gt(lastId),
                        ).orderBy(statsRankBattleEntity.id.asc())
                        .limit(BattleStatsConstants.READ_PAGE_SIZE)
                        .fetch()

                if (battles.isEmpty()) break
                totalRead += battles.size
                lastId = battles.last().id!!
                consume(battles)
            }

            return totalRead
        }
    }
}
