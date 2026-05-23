package com.imstargg.core.batch.job.statistics

import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class BrawlerBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(
        date: LocalDate,
        consume: (List<StatsResultBattleEntity>) -> Unit,
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
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.eq(date),
                            statsResultBattleEntity.id.gt(lastId),
                        ).orderBy(statsResultBattleEntity.id.asc())
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
