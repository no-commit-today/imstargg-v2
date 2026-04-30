package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerItemOwnershipStatsWriter(
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
) {
    @Transactional
    fun write(result: BrawlerItemOwnershipStatsResult) {
        gearRepository.deleteAllInBatch()
        gadgetRepository.deleteAllInBatch()
        starPowerRepository.deleteAllInBatch()
        hyperChargeRepository.deleteAllInBatch()
        buffyRepository.deleteAllInBatch()

        result.gears.chunked(BATCH_SIZE).forEach { gearRepository.saveAll(it) }
        result.gadgets.chunked(BATCH_SIZE).forEach { gadgetRepository.saveAll(it) }
        result.starPowers.chunked(BATCH_SIZE).forEach { starPowerRepository.saveAll(it) }
        result.hyperCharges.chunked(BATCH_SIZE).forEach { hyperChargeRepository.saveAll(it) }
        result.buffies.chunked(BATCH_SIZE).forEach { buffyRepository.saveAll(it) }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
