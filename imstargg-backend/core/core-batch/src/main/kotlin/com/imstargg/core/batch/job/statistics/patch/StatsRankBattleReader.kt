package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity.Companion.statsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import java.time.Instant
import java.time.LocalDate

class StatsRankBattleReader(
    private val entityManagerFactory: EntityManagerFactory,
    private val pageSize: Long,
) : StatsBattleReader<StatsRankBattleEntity> {
    override fun findBattleIdCap(
        createdAtCap: Instant,
        startBattleDate: LocalDate,
    ): Long? =
        entityManagerFactory.createEntityManager().use { em ->
            JPAQueryFactory(em)
                .select(statsRankBattleEntity.id.max())
                .from(statsRankBattleEntity)
                .where(
                    statsRankBattleEntity.battleDate.goe(startBattleDate),
                    statsRankBattleEntity.createdAt.loe(createdAtCap),
                ).fetchOne()
        }

    override fun findEarliestBattleDate(): LocalDate? =
        entityManagerFactory.createEntityManager().use { em ->
            JPAQueryFactory(em)
                .select(statsRankBattleEntity.battleDate.min())
                .from(statsRankBattleEntity)
                .fetchOne()
        }

    override fun read(
        dateRange: PatchDateRange,
        afterBattleId: Long,
        battleIdCap: Long,
        consume: (List<StatsRankBattleEntity>) -> Unit,
    ) {
        entityManagerFactory.createEntityManager().use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = afterBattleId

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsRankBattleEntity)
                        .where(
                            statsRankBattleEntity.battleDate.goe(dateRange.startDate),
                            dateRange.endDate?.let { statsRankBattleEntity.battleDate.loe(it) },
                            statsRankBattleEntity.id.gt(lastId),
                            statsRankBattleEntity.id.loe(battleIdCap),
                        ).orderBy(statsRankBattleEntity.id.asc())
                        .limit(pageSize)
                        .fetch()

                if (battles.isEmpty()) break
                lastId = battles.last().id!!
                consume(battles)
            }
        }
    }
}
