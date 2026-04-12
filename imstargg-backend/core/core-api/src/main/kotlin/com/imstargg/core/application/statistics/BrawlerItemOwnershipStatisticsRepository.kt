package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerBuffyOwnershipRate
import com.imstargg.core.domain.statistics.BrawlerItemOwnershipRate
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class BrawlerItemOwnershipStatisticsRepository(
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
) {
    fun findGearOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        gearRepository
            .findAllByBrawlerBrawlStarsIdAndTrophyRange(
                brawlerBrawlStarsId = brawlerId.value,
                trophyRange = trophyRange,
            ).map {
                BrawlerItemOwnershipRate(
                    itemId = BrawlStarsId(it.gearBrawlStarsId),
                    ownerCount = it.ownerCount,
                    totalCount = it.totalCount,
                )
            }

    fun findGadgetOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        gadgetRepository
            .findAllByBrawlerBrawlStarsIdAndTrophyRange(
                brawlerBrawlStarsId = brawlerId.value,
                trophyRange = trophyRange,
            ).map {
                BrawlerItemOwnershipRate(
                    itemId = BrawlStarsId(it.gadgetBrawlStarsId),
                    ownerCount = it.ownerCount,
                    totalCount = it.totalCount,
                )
            }

    fun findStarPowerOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        starPowerRepository
            .findAllByBrawlerBrawlStarsIdAndTrophyRange(
                brawlerBrawlStarsId = brawlerId.value,
                trophyRange = trophyRange,
            ).map {
                BrawlerItemOwnershipRate(
                    itemId = BrawlStarsId(it.starPowerBrawlStarsId),
                    ownerCount = it.ownerCount,
                    totalCount = it.totalCount,
                )
            }

    fun findHyperChargeOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerItemOwnershipRate> =
        hyperChargeRepository
            .findAllByBrawlerBrawlStarsIdAndTrophyRange(
                brawlerBrawlStarsId = brawlerId.value,
                trophyRange = trophyRange,
            ).map {
                BrawlerItemOwnershipRate(
                    itemId = BrawlStarsId(it.hyperChargeBrawlStarsId),
                    ownerCount = it.ownerCount,
                    totalCount = it.totalCount,
                )
            }

    fun findBuffyOwnership(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): List<BrawlerBuffyOwnershipRate> =
        buffyRepository
            .findAllByBrawlerBrawlStarsIdAndTrophyRange(
                brawlerBrawlStarsId = brawlerId.value,
                trophyRange = trophyRange,
            ).map {
                BrawlerBuffyOwnershipRate(
                    buffyType = it.buffyType,
                    ownerCount = it.ownerCount,
                    totalCount = it.totalCount,
                )
            }
}
