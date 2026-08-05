package com.imstargg.core.batch.domain.statistics.ownership

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.OwnershipCountEmbedded

class BrawlerItemOwnershipStatsAccumulator {
    private val totalCounts = mutableMapOf<BrawlerTrophyKey, Long>()
    private val gearCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val gadgetCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val starPowerCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val hyperChargeCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
    private val buffyCounts = mutableMapOf<BrawlerTrophyBuffyKey, Long>()

    fun add(snapshot: PlayerBrawlerOwnershipSnapshot) {
        val trophyRanges = BrawlerTrophyRange.resolveAll(snapshot.trophies)

        for (trophyRange in trophyRanges) {
            val totalKey =
                BrawlerTrophyKey(
                    brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                    trophyRange = trophyRange,
                )
            totalCounts.merge(totalKey, 1L) { a, b -> a + b }

            snapshot.gearBrawlStarsIds.forEach { gearBrawlStarsId ->
                val key =
                    BrawlerTrophyItemKey(
                        brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                        trophyRange = trophyRange,
                        itemBrawlStarsId = gearBrawlStarsId,
                    )
                gearCounts.merge(key, 1L) { a, b -> a + b }
            }

            snapshot.gadgetBrawlStarsIds.forEach { gadgetBrawlStarsId ->
                val key =
                    BrawlerTrophyItemKey(
                        brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                        trophyRange = trophyRange,
                        itemBrawlStarsId = gadgetBrawlStarsId,
                    )
                gadgetCounts.merge(key, 1L) { a, b -> a + b }
            }

            snapshot.starPowerBrawlStarsIds.forEach { starPowerBrawlStarsId ->
                val key =
                    BrawlerTrophyItemKey(
                        brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                        trophyRange = trophyRange,
                        itemBrawlStarsId = starPowerBrawlStarsId,
                    )
                starPowerCounts.merge(key, 1L) { a, b -> a + b }
            }

            snapshot.hyperChargeBrawlStarsIds.forEach { hyperChargeBrawlStarsId ->
                val key =
                    BrawlerTrophyItemKey(
                        brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                        trophyRange = trophyRange,
                        itemBrawlStarsId = hyperChargeBrawlStarsId,
                    )
                hyperChargeCounts.merge(key, 1L) { a, b -> a + b }
            }

            snapshot.buffyTypes.forEach { buffyType ->
                val key =
                    BrawlerTrophyBuffyKey(
                        brawlerBrawlStarsId = snapshot.brawlerBrawlStarsId,
                        trophyRange = trophyRange,
                        buffyType = buffyType,
                    )
                buffyCounts.merge(key, 1L) { a, b -> a + b }
            }
        }
    }

    fun toResult(): BrawlerItemOwnershipStatsResult {
        val gearEntities =
            gearCounts.map { (key, ownerCount) ->
                BrawlerGearOwnershipStatsEntity(
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    trophyRange = key.trophyRange,
                    gearBrawlStarsId = key.itemBrawlStarsId,
                    ownershipCount = ownershipCountOf(key.toBrawlerTrophyKey(), ownerCount),
                )
            }

        val gadgetEntities =
            gadgetCounts.map { (key, ownerCount) ->
                BrawlerGadgetOwnershipStatsEntity(
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    trophyRange = key.trophyRange,
                    gadgetBrawlStarsId = key.itemBrawlStarsId,
                    ownershipCount = ownershipCountOf(key.toBrawlerTrophyKey(), ownerCount),
                )
            }

        val starPowerEntities =
            starPowerCounts.map { (key, ownerCount) ->
                BrawlerStarPowerOwnershipStatsEntity(
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    trophyRange = key.trophyRange,
                    starPowerBrawlStarsId = key.itemBrawlStarsId,
                    ownershipCount = ownershipCountOf(key.toBrawlerTrophyKey(), ownerCount),
                )
            }

        val hyperChargeEntities =
            hyperChargeCounts.map { (key, ownerCount) ->
                BrawlerHyperChargeOwnershipStatsEntity(
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    trophyRange = key.trophyRange,
                    hyperChargeBrawlStarsId = key.itemBrawlStarsId,
                    ownershipCount = ownershipCountOf(key.toBrawlerTrophyKey(), ownerCount),
                )
            }

        val buffyEntities =
            buffyCounts.map { (key, ownerCount) ->
                BrawlerBuffyOwnershipStatsEntity(
                    brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                    trophyRange = key.trophyRange,
                    buffyType = key.buffyType,
                    ownershipCount = ownershipCountOf(key.toBrawlerTrophyKey(), ownerCount),
                )
            }

        return BrawlerItemOwnershipStatsResult(
            gears = gearEntities,
            gadgets = gadgetEntities,
            starPowers = starPowerEntities,
            hyperCharges = hyperChargeEntities,
            buffies = buffyEntities,
        )
    }

    /**
     * [add] 가 아이템 카운트를 올리기 전에 항상 같은 [BrawlerTrophyKey] 의 총계를 먼저 올리므로
     * 총계 누락은 발생할 수 없다. 0 으로 대체하면 소유율이 조용히 0 이 되므로 실패시킨다.
     */
    private fun ownershipCountOf(
        totalKey: BrawlerTrophyKey,
        ownerCount: Long,
    ): OwnershipCountEmbedded =
        OwnershipCountEmbedded(
            ownerCount = ownerCount,
            totalCount = totalCounts[totalKey] ?: error("총계 카운트가 없습니다. key=$totalKey"),
        )
}
