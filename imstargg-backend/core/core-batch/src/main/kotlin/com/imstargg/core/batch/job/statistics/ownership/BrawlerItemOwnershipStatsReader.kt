package com.imstargg.core.batch.job.statistics.ownership

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

                val playerBrawlers =
                    queryFactory
                        .selectFrom(playerBrawlerEntity)
                        .join(playerBrawlerEntity.player, playerEntity)
                        .where(playerEntity.brawlStarsTag.`in`(statsPlayers.map { it.brawlStarsTag }))
                        .fetch()

                if (playerBrawlers.isEmpty()) continue

                val playerBrawlerIds = playerBrawlers.mapNotNull { it.id }
                if (playerBrawlerIds.isEmpty()) continue

                val gearByPlayerBrawlerId =
                    groupByPlayerBrawlerId(
                        queryFactory
                            .selectFrom(playerBrawlerGearEntity)
                            .where(playerBrawlerGearEntity.playerBrawler.id.`in`(playerBrawlerIds))
                            .fetch(),
                    ) { it.playerBrawler.id }
                val gadgetByPlayerBrawlerId =
                    groupByPlayerBrawlerId(
                        queryFactory
                            .selectFrom(playerBrawlerGadgetEntity)
                            .where(playerBrawlerGadgetEntity.playerBrawler.id.`in`(playerBrawlerIds))
                            .fetch(),
                    ) { it.playerBrawler.id }
                val starPowerByPlayerBrawlerId =
                    groupByPlayerBrawlerId(
                        queryFactory
                            .selectFrom(playerBrawlerStarPowerEntity)
                            .where(playerBrawlerStarPowerEntity.playerBrawler.id.`in`(playerBrawlerIds))
                            .fetch(),
                    ) { it.playerBrawler.id }
                val hyperChargeByPlayerBrawlerId =
                    groupByPlayerBrawlerId(
                        queryFactory
                            .selectFrom(playerBrawlerHyperChargeEntity)
                            .where(playerBrawlerHyperChargeEntity.playerBrawler.id.`in`(playerBrawlerIds))
                            .fetch(),
                    ) { it.playerBrawler.id }
                val buffyByPlayerBrawlerId =
                    groupByPlayerBrawlerId(
                        queryFactory
                            .selectFrom(playerBrawlerBuffyEntity)
                            .where(playerBrawlerBuffyEntity.playerBrawler.id.`in`(playerBrawlerIds))
                            .fetch(),
                    ) { it.playerBrawler.id }

                val snapshots =
                    playerBrawlers.mapNotNull { playerBrawler ->
                        val playerBrawlerId = playerBrawler.id ?: return@mapNotNull null
                        PlayerBrawlerOwnershipSnapshot(
                            playerBrawlerId = playerBrawlerId,
                            brawlerBrawlStarsId = playerBrawler.brawlerBrawlStarsId,
                            trophies = playerBrawler.trophies,
                            gearBrawlStarsIds =
                                gearByPlayerBrawlerId[playerBrawlerId]
                                    .orEmpty()
                                    .map { it.gearBrawlStarsId },
                            gadgetBrawlStarsIds =
                                gadgetByPlayerBrawlerId[playerBrawlerId]
                                    .orEmpty()
                                    .map { it.gadgetBrawlStarsId },
                            starPowerBrawlStarsIds =
                                starPowerByPlayerBrawlerId[playerBrawlerId]
                                    .orEmpty()
                                    .map { it.starPowerBrawlStarsId },
                            hyperChargeBrawlStarsIds =
                                hyperChargeByPlayerBrawlerId[playerBrawlerId]
                                    .orEmpty()
                                    .map { it.hyperChargeBrawlStarsId },
                            buffyTypes =
                                buffyByPlayerBrawlerId[playerBrawlerId]
                                    .orEmpty()
                                    .map { it.buffyType },
                        )
                    }

                if (snapshots.isNotEmpty()) {
                    consume(snapshots)
                }
            }

            return totalRead
        }
    }

    private fun <T> groupByPlayerBrawlerId(
        items: List<T>,
        playerBrawlerIdOf: (T) -> Long?,
    ): Map<Long, List<T>> =
        items
            .mapNotNull { item ->
                playerBrawlerIdOf(item)?.let { playerBrawlerId ->
                    playerBrawlerId to item
                }
            }.groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )

    companion object {
        private const val PAGE_SIZE = 1000L
    }
}
