package com.imstargg.core.batch.job.statistics.enemybattleresult

import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.enums.WeekRange
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity.Companion.statsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

/**
 * 주 버킷의 결과형 전투를 id 키셋으로 페이지([BattleStatsConstants.READ_PAGE_SIZE]건)씩 읽는다.
 * 이벤트별로 나누지 않고 버킷 전체를 한 번에 스트리밍한다.
 */
@Component
class BrawlerEnemyBattleResultStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(
        weekRange: WeekRange,
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
                            statsResultBattleEntity.battleDate.between(weekRange.start, weekRange.end),
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
