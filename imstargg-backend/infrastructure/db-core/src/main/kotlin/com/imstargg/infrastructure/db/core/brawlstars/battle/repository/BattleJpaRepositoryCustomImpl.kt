package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import com.imstargg.infrastructure.db.core.brawlstars.battle.QBattleEntity.Companion.battleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.QBattleTeamPlayerEntity.Companion.battleTeamPlayerEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.time.Instant

internal class BattleJpaRepositoryCustomImpl(
    entityManager: EntityManager,
) : BattleJpaRepositoryCustom {
    private val queryFactory = JPAQueryFactory(entityManager)

    override fun aggregatePlayerBrawlerResults(
        playerId: Long,
        playerTag: String,
        battleTypeCode: String,
        startAt: Instant,
        endAt: Instant,
    ): List<PlayerBrawlerResultProjection> {
        val brawlerId = battleTeamPlayerEntity.brawler.brawlStarsId
        val result = battleEntity.result
        val count = battleTeamPlayerEntity.id.count()

        return queryFactory
            .select(brawlerId, result, count)
            .from(battleTeamPlayerEntity)
            .join(battleTeamPlayerEntity.battle, battleEntity)
            .where(
                battleEntity.player.id.eq(playerId),
                battleTeamPlayerEntity.brawlStarsTag.eq(playerTag),
                battleEntity.type.eq(battleTypeCode),
                battleEntity.battleAt.goe(startAt),
                battleEntity.battleAt.lt(endAt),
                battleEntity.result.isNotNull,
            ).groupBy(brawlerId, result)
            .fetch()
            .map { tuple ->
                PlayerBrawlerResultProjection(
                    brawlerBrawlStarsId = tuple[brawlerId]!!,
                    result = tuple[result]!!,
                    count = tuple[count]!!,
                )
            }
    }
}
