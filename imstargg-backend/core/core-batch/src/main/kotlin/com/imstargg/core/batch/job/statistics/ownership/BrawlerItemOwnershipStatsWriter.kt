package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsResult
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import org.springframework.stereotype.Component

@Component
class BrawlerItemOwnershipStatsWriter(
    private val gearRepository: BrawlerGearOwnershipStatsJpaRepository,
    private val gadgetRepository: BrawlerGadgetOwnershipStatsJpaRepository,
    private val starPowerRepository: BrawlerStarPowerOwnershipStatsJpaRepository,
    private val hyperChargeRepository: BrawlerHyperChargeOwnershipStatsJpaRepository,
    private val buffyRepository: BrawlerBuffyOwnershipStatsJpaRepository,
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    fun write(result: BrawlerItemOwnershipStatsResult) {
        replaceTable(
            deleteAll = { gearRepository.deleteAllInBatch() },
            entities = result.gears,
            saveAllAndFlush = { gearRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { gadgetRepository.deleteAllInBatch() },
            entities = result.gadgets,
            saveAllAndFlush = { gadgetRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { starPowerRepository.deleteAllInBatch() },
            entities = result.starPowers,
            saveAllAndFlush = { starPowerRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { hyperChargeRepository.deleteAllInBatch() },
            entities = result.hyperCharges,
            saveAllAndFlush = { hyperChargeRepository.saveAllAndFlush(it) },
        )
        replaceTable(
            deleteAll = { buffyRepository.deleteAllInBatch() },
            entities = result.buffies,
            saveAllAndFlush = { buffyRepository.saveAllAndFlush(it) },
        )
    }

    private fun <T : Any> replaceTable(
        deleteAll: () -> Unit,
        entities: List<T>,
        saveAllAndFlush: (List<T>) -> Unit,
    ) {
        transactionRunner.run { entityManager, _ ->
            deleteAll()
            entities.chunked(BATCH_SIZE).forEach { batch ->
                saveAllAndFlush(batch)
                entityManager.clear()
            }
        }
    }

    companion object {
        private const val BATCH_SIZE = 1000
    }
}
