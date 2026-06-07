package com.imstargg.core.batch.job.statistics.battlerank

import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleRankStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(
        date: LocalDate,
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
                            statsRankBattleEntity.battleDate.eq(date),
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
