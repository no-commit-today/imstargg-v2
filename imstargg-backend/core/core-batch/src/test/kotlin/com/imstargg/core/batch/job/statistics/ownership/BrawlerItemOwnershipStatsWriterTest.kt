package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerBuffyOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsJpaRepository
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsJpaRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

class BrawlerItemOwnershipStatsWriterTest {
    private val gearRepository = mockk<BrawlerGearOwnershipStatsJpaRepository>()
    private val gadgetRepository = mockk<BrawlerGadgetOwnershipStatsJpaRepository>()
    private val starPowerRepository = mockk<BrawlerStarPowerOwnershipStatsJpaRepository>()
    private val hyperChargeRepository = mockk<BrawlerHyperChargeOwnershipStatsJpaRepository>()
    private val buffyRepository = mockk<BrawlerBuffyOwnershipStatsJpaRepository>()
    private val txManager = mockk<PlatformTransactionManager>()
    private val entityManager = mockk<EntityManager>()
    private val transactionRunner = JpaBatchTransactionRunner(txManager, entityManager)
    private val sut =
        BrawlerItemOwnershipStatsWriter(
            gearRepository = gearRepository,
            gadgetRepository = gadgetRepository,
            starPowerRepository = starPowerRepository,
            hyperChargeRepository = hyperChargeRepository,
            buffyRepository = buffyRepository,
            transactionRunner = transactionRunner,
        )

    @Test
    fun `테이블별 짧은 transaction 에서 기존 집계를 삭제한 뒤 chunk 단위로 flush 하고 영속성 컨텍스트를 비운다`() {
        // given
        val gearBatches = mutableListOf<List<BrawlerGearOwnershipStatsEntity>>()
        every {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        } answers { mockk<TransactionStatus>(relaxed = true) }
        every { txManager.commit(any()) } just Runs
        every { gearRepository.deleteAllInBatch() } just Runs
        every { gadgetRepository.deleteAllInBatch() } just Runs
        every { starPowerRepository.deleteAllInBatch() } just Runs
        every { hyperChargeRepository.deleteAllInBatch() } just Runs
        every { buffyRepository.deleteAllInBatch() } just Runs
        every { gearRepository.saveAllAndFlush(any<Iterable<BrawlerGearOwnershipStatsEntity>>()) } answers {
            val batch = firstArg<Iterable<BrawlerGearOwnershipStatsEntity>>().toList()
            gearBatches.add(batch)
            batch
        }
        every { entityManager.clear() } just Runs

        val result =
            BrawlerItemOwnershipStatsResult(
                gears = (1..1001).map { gearStatsEntity(it.toLong()) },
                gadgets = emptyList(),
                starPowers = emptyList(),
                hyperCharges = emptyList(),
                buffies = emptyList(),
            )

        // when
        sut.write(result)

        // then
        verify(exactly = 1) { gearRepository.deleteAllInBatch() }
        verify(exactly = 1) { gadgetRepository.deleteAllInBatch() }
        verify(exactly = 1) { starPowerRepository.deleteAllInBatch() }
        verify(exactly = 1) { hyperChargeRepository.deleteAllInBatch() }
        verify(exactly = 1) { buffyRepository.deleteAllInBatch() }
        verify(exactly = 2) { entityManager.clear() }
        verify(exactly = 5) {
            txManager.getTransaction(
                match { it.propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW },
            )
        }
        verify(exactly = 5) { txManager.commit(any()) }
        assertThat(gearBatches.map { it.size }).containsExactly(1000, 1)
    }

    private fun gearStatsEntity(gearBrawlStarsId: Long): BrawlerGearOwnershipStatsEntity =
        BrawlerGearOwnershipStatsEntity(
            brawlerBrawlStarsId = 16000000,
            trophyRange = BrawlerTrophyRange.TROPHY_500_PLUS,
            gearBrawlStarsId = gearBrawlStarsId,
            ownerCount = 1,
            totalCount = 1,
        )
}
