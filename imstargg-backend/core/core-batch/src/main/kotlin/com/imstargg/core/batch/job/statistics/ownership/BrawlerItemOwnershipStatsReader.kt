package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.domain.statistics.ownership.PlayerBrawlerOwnershipSnapshot
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerBuffyEntity.Companion.playerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerEntity.Companion.playerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGadgetEntity.Companion.playerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGearEntity.Companion.playerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerHyperChargeEntity.Companion.playerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerStarPowerEntity.Companion.playerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerEntity.Companion.playerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.stereotype.Component

/**
 * stats_player 를 키셋으로 순회하며, 페이지의 플레이어 태그(최대 [PAGE_SIZE]개)를 기준으로
 * player_brawler 와 아이템 5종을 조회해 소유 스냅샷을 만든다.
 *
 * 아이템 조회는 player_brawler id 목록(IN, 페이지당 최대 수만 개)이 아니라
 * 플레이어 태그 join(IN 크기 = 페이지 크기 고정) 으로 필터한다.
 * 아이템 5종의 조회 반복은 의도된 명시성이다 (2026-05-22 설계 참조).
 */
@Component
class BrawlerItemOwnershipStatsReader(
    private val entityManagerFactory: EntityManagerFactory,
) {
    fun read(consume: (List<PlayerBrawlerOwnershipSnapshot>) -> Unit): Long {
        val entityManager = entityManagerFactory.createEntityManager()

        entityManager.use { em ->
            val queryFactory = JPAQueryFactory(em)
            var lastId = 0L
            var totalRead = 0L

            while (true) {
                em.clear()

                val statsPlayers =
                    queryFactory
                        .selectFrom(statsPlayerEntity)
                        .where(statsPlayerEntity.id.gt(lastId))
                        .orderBy(statsPlayerEntity.id.asc())
                        .limit(PAGE_SIZE)
                        .fetch()

                if (statsPlayers.isEmpty()) break
                totalRead += statsPlayers.size
                lastId = statsPlayers.last().id!!
                val tags = statsPlayers.map { it.brawlStarsTag }

                val playerBrawlerRows =
                    queryFactory
                        .select(
                            playerBrawlerEntity.id,
                            playerBrawlerEntity.brawlerBrawlStarsId,
                            playerBrawlerEntity.trophies,
                        ).from(playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()

                if (playerBrawlerRows.isEmpty()) continue

                val gearIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerGearEntity.playerBrawler.id, playerBrawlerGearEntity.gearBrawlStarsId)
                        .from(playerBrawlerGearEntity)
                        .join(playerBrawlerGearEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerGearEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerGearEntity.gearBrawlStarsId)!! },
                        )
                val gadgetIdsByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerGadgetEntity.playerBrawler.id, playerBrawlerGadgetEntity.gadgetBrawlStarsId)
                        .from(playerBrawlerGadgetEntity)
                        .join(playerBrawlerGadgetEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerGadgetEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerGadgetEntity.gadgetBrawlStarsId)!! },
                        )
                val starPowerIdsByPlayerBrawlerId =
                    queryFactory
                        .select(
                            playerBrawlerStarPowerEntity.playerBrawler.id,
                            playerBrawlerStarPowerEntity.starPowerBrawlStarsId,
                        ).from(playerBrawlerStarPowerEntity)
                        .join(playerBrawlerStarPowerEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerStarPowerEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerStarPowerEntity.starPowerBrawlStarsId)!! },
                        )
                val hyperChargeIdsByPlayerBrawlerId =
                    queryFactory
                        .select(
                            playerBrawlerHyperChargeEntity.playerBrawler.id,
                            playerBrawlerHyperChargeEntity.hyperChargeBrawlStarsId,
                        ).from(playerBrawlerHyperChargeEntity)
                        .join(playerBrawlerHyperChargeEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerHyperChargeEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerHyperChargeEntity.hyperChargeBrawlStarsId)!! },
                        )
                val buffyTypesByPlayerBrawlerId =
                    queryFactory
                        .select(playerBrawlerBuffyEntity.playerBrawler.id, playerBrawlerBuffyEntity.buffyType)
                        .from(playerBrawlerBuffyEntity)
                        .join(playerBrawlerBuffyEntity.playerBrawler, playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(tags))
                        .fetch()
                        .groupBy(
                            keySelector = { it.get(playerBrawlerBuffyEntity.playerBrawler.id)!! },
                            valueTransform = { it.get(playerBrawlerBuffyEntity.buffyType)!! },
                        )

                val snapshots =
                    playerBrawlerRows.mapNotNull { row ->
                        val playerBrawlerId = row.get(playerBrawlerEntity.id) ?: return@mapNotNull null
                        PlayerBrawlerOwnershipSnapshot(
                            playerBrawlerId = playerBrawlerId,
                            brawlerBrawlStarsId = row.get(playerBrawlerEntity.brawlerBrawlStarsId)!!,
                            trophies = row.get(playerBrawlerEntity.trophies)!!,
                            gearBrawlStarsIds = gearIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            gadgetBrawlStarsIds = gadgetIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            starPowerBrawlStarsIds = starPowerIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            hyperChargeBrawlStarsIds = hyperChargeIdsByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                            buffyTypes = buffyTypesByPlayerBrawlerId[playerBrawlerId].orEmpty(),
                        )
                    }

                if (snapshots.isNotEmpty()) {
                    consume(snapshots)
                }
            }

            return totalRead
        }
    }

    companion object {
        private const val PAGE_SIZE = 1000L
    }
}
