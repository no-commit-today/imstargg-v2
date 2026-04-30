package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrawlerItemOwnershipStatsWriter(
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun write(result: BrawlerItemOwnershipStatsResult) {
        gearRepository.deleteAllInBatch()
        gadgetRepository.deleteAllInBatch()
        starPowerRepository.deleteAllInBatch()
        hyperChargeRepository.deleteAllInBatch()
        buffyRepository.deleteAllInBatch()

        saveAllInChunks(result.gears) { gearRepository.saveAllAndFlush(it) }
        saveAllInChunks(result.gadgets) { gadgetRepository.saveAllAndFlush(it) }
        saveAllInChunks(result.starPowers) { starPowerRepository.saveAllAndFlush(it) }
        saveAllInChunks(result.hyperCharges) { hyperChargeRepository.saveAllAndFlush(it) }
        saveAllInChunks(result.buffies) { buffyRepository.saveAllAndFlush(it) }
    }

    private fun <T : Any> saveAllInChunks(
        entities: List<T>,
        saveAllAndFlush: (List<T>) -> Unit,
    ) {
        entities.chunked(BATCH_SIZE).forEach { batch ->
            saveAllAndFlush(batch)
            entityManager.clear()
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
