package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import java.time.Instant
import java.time.LocalDate

class StatsResultBattleReader(
    private val entityManagerFactory: EntityManagerFactory,
    private val pageSize: Long,
) : StatsBattleReader<StatsResultBattleEntity> {
    override fun findBattleIdCap(
        createdAtCap: Instant,
        startBattleDate: LocalDate,
    ): Long? =
        entityManagerFactory.createEntityManager().use { em ->
            JPAQueryFactory(em)
                .select(statsResultBattleEntity.id.max())
                .from(statsResultBattleEntity)
                .where(
                    statsResultBattleEntity.battleDate.goe(startBattleDate),
                    statsResultBattleEntity.createdAt.loe(createdAtCap),
                ).fetchOne()
        }

    override fun findEarliestBattleDate(): LocalDate? =
        entityManagerFactory.createEntityManager().use { em ->
            JPAQueryFactory(em)
                .select(statsResultBattleEntity.battleDate.min())
                .from(statsResultBattleEntity)
                .fetchOne()
        }

    override fun read(
        dateRange: PatchDateRange,
        afterBattleId: Long,
        battleIdCap: Long,
        consume: (List<StatsResultBattleEntity>) -> Unit,
    ) {
        entityManagerFactory.createEntityManager().use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = afterBattleId

            while (true) {
                em.clear()

                val battles =
                    queryFactory
                        .selectFrom(statsResultBattleEntity)
                        .where(
                            statsResultBattleEntity.battleDate.goe(dateRange.startDate),
                            dateRange.endDate?.let { statsResultBattleEntity.battleDate.loe(it) },
                            statsResultBattleEntity.id.gt(lastId),
                            statsResultBattleEntity.id.loe(battleIdCap),
                        ).orderBy(statsResultBattleEntity.id.asc())
                        .limit(pageSize)
                        .fetch()

                if (battles.isEmpty()) break
                lastId = battles.last().id!!
                consume(battles)
            }
        }
    }
}
