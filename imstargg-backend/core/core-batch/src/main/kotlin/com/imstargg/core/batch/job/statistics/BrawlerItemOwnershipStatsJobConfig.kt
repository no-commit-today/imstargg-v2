package com.imstargg.core.batch.job.statistics

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerEntity.Companion.playerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGadgetEntity.Companion.playerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerGearEntity.Companion.playerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerHyperChargeEntity.Companion.playerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerBrawlerStarPowerEntity.Companion.playerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.QPlayerEntity.Companion.playerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.BrawlerStarPowerOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.QBrawlerGadgetOwnershipStatsEntity.Companion.brawlerGadgetOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.QBrawlerGearOwnershipStatsEntity.Companion.brawlerGearOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.QBrawlerHyperChargeOwnershipStatsEntity.Companion.brawlerHyperChargeOwnershipStatsEntity
import com.imstargg.infrastructure.db.core.statistics.brawler.QBrawlerStarPowerOwnershipStatsEntity.Companion.brawlerStarPowerOwnershipStatsEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManagerFactory
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerItemOwnershipStatsJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "brawlerItemOwnershipStatsJob"
        private const val STEP_NAME = "brawlerItemOwnershipStatsStep"
        private const val PAGE_SIZE = 1000L
        private const val BATCH_SIZE = 1000
    }

    @Bean(JOB_NAME)
    fun brawlerItemOwnershipStatsJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerItemOwnershipStatsStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun brawlerItemOwnershipStatsStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val entityManager = entityManagerFactory.createEntityManager()

                entityManager.use { em ->
                    val queryFactory = JPAQueryFactory(em)

                    val totalCounts = mutableMapOf<BrawlerTrophyKey, Long>()
                    val gearCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
                    val gadgetCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
                    val starPowerCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()
                    val hyperChargeCounts = mutableMapOf<BrawlerTrophyItemKey, Long>()

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

                        val playerBrawlers =
                            queryFactory
                                .selectFrom(playerBrawlerEntity)
                                .join(playerBrawlerEntity.player, playerEntity)
                                .where(playerEntity.brawlStarsTag.`in`(tags))
                                .fetch()

                        if (playerBrawlers.isEmpty()) continue

                        val playerBrawlerIds = playerBrawlers.mapNotNull { it.id }

                        for (pb in playerBrawlers) {
                            val trophyRanges = BrawlerTrophyRange.resolveAll(pb.trophies)
                            for (trophyRange in trophyRanges) {
                                val key =
                                    BrawlerTrophyKey(
                                        brawlerBrawlStarsId = pb.brawlerBrawlStarsId,
                                        trophyRange = trophyRange,
                                    )
                                totalCounts.merge(key, 1L) { a, b -> a + b }
                            }
                        }

                        val gears =
                            queryFactory
                                .selectFrom(playerBrawlerGearEntity)
                                .where(playerBrawlerGearEntity.playerBrawler.id.`in`(playerBrawlerIds))
                                .fetch()

                        val gadgets =
                            queryFactory
                                .selectFrom(playerBrawlerGadgetEntity)
                                .where(playerBrawlerGadgetEntity.playerBrawler.id.`in`(playerBrawlerIds))
                                .fetch()

                        val starPowers =
                            queryFactory
                                .selectFrom(playerBrawlerStarPowerEntity)
                                .where(playerBrawlerStarPowerEntity.playerBrawler.id.`in`(playerBrawlerIds))
                                .fetch()

                        val hyperCharges =
                            queryFactory
                                .selectFrom(playerBrawlerHyperChargeEntity)
                                .where(playerBrawlerHyperChargeEntity.playerBrawler.id.`in`(playerBrawlerIds))
                                .fetch()

                        val playerBrawlerById = playerBrawlers.associateBy { it.id }

                        for (gear in gears) {
                            val pb = playerBrawlerById[gear.playerBrawler.id] ?: continue
                            val trophyRanges = BrawlerTrophyRange.resolveAll(pb.trophies)
                            for (trophyRange in trophyRanges) {
                                val key =
                                    BrawlerTrophyItemKey(
                                        brawlerBrawlStarsId = pb.brawlerBrawlStarsId,
                                        trophyRange = trophyRange,
                                        itemBrawlStarsId = gear.gearBrawlStarsId,
                                    )
                                gearCounts.merge(key, 1L) { a, b -> a + b }
                            }
                        }

                        for (gadget in gadgets) {
                            val pb = playerBrawlerById[gadget.playerBrawler.id] ?: continue
                            val trophyRanges = BrawlerTrophyRange.resolveAll(pb.trophies)
                            for (trophyRange in trophyRanges) {
                                val key =
                                    BrawlerTrophyItemKey(
                                        brawlerBrawlStarsId = pb.brawlerBrawlStarsId,
                                        trophyRange = trophyRange,
                                        itemBrawlStarsId = gadget.gadgetBrawlStarsId,
                                    )
                                gadgetCounts.merge(key, 1L) { a, b -> a + b }
                            }
                        }

                        for (starPower in starPowers) {
                            val pb = playerBrawlerById[starPower.playerBrawler.id] ?: continue
                            val trophyRanges = BrawlerTrophyRange.resolveAll(pb.trophies)
                            for (trophyRange in trophyRanges) {
                                val key =
                                    BrawlerTrophyItemKey(
                                        brawlerBrawlStarsId = pb.brawlerBrawlStarsId,
                                        trophyRange = trophyRange,
                                        itemBrawlStarsId = starPower.starPowerBrawlStarsId,
                                    )
                                starPowerCounts.merge(key, 1L) { a, b -> a + b }
                            }
                        }

                        for (hyperCharge in hyperCharges) {
                            val pb = playerBrawlerById[hyperCharge.playerBrawler.id] ?: continue
                            val trophyRanges = BrawlerTrophyRange.resolveAll(pb.trophies)
                            for (trophyRange in trophyRanges) {
                                val key =
                                    BrawlerTrophyItemKey(
                                        brawlerBrawlStarsId = pb.brawlerBrawlStarsId,
                                        trophyRange = trophyRange,
                                        itemBrawlStarsId = hyperCharge.hyperChargeBrawlStarsId,
                                    )
                                hyperChargeCounts.merge(key, 1L) { a, b -> a + b }
                            }
                        }

                        log.debug("[{}] 읽기 진행 - 누적: {}건", JOB_NAME, totalRead)
                    }

                    em.transaction.begin()
                    queryFactory.delete(brawlerGearOwnershipStatsEntity).execute()
                    queryFactory.delete(brawlerGadgetOwnershipStatsEntity).execute()
                    queryFactory.delete(brawlerStarPowerOwnershipStatsEntity).execute()
                    queryFactory.delete(brawlerHyperChargeOwnershipStatsEntity).execute()
                    em.transaction.commit()

                    val gearEntities =
                        gearCounts.map { (key, ownerCount) ->
                            BrawlerGearOwnershipStatsEntity(
                                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                                trophyRange = key.trophyRange,
                                gearBrawlStarsId = key.itemBrawlStarsId,
                                ownerCount = ownerCount,
                                totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                            )
                        }

                    val gadgetEntities =
                        gadgetCounts.map { (key, ownerCount) ->
                            BrawlerGadgetOwnershipStatsEntity(
                                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                                trophyRange = key.trophyRange,
                                gadgetBrawlStarsId = key.itemBrawlStarsId,
                                ownerCount = ownerCount,
                                totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                            )
                        }

                    val starPowerEntities =
                        starPowerCounts.map { (key, ownerCount) ->
                            BrawlerStarPowerOwnershipStatsEntity(
                                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                                trophyRange = key.trophyRange,
                                starPowerBrawlStarsId = key.itemBrawlStarsId,
                                ownerCount = ownerCount,
                                totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                            )
                        }

                    val hyperChargeEntities =
                        hyperChargeCounts.map { (key, ownerCount) ->
                            BrawlerHyperChargeOwnershipStatsEntity(
                                brawlerBrawlStarsId = key.brawlerBrawlStarsId,
                                trophyRange = key.trophyRange,
                                hyperChargeBrawlStarsId = key.itemBrawlStarsId,
                                ownerCount = ownerCount,
                                totalCount = totalCounts[key.toBrawlerTrophyKey()] ?: 0L,
                            )
                        }

                    val allEntities = gearEntities + gadgetEntities + starPowerEntities + hyperChargeEntities
                    allEntities.chunked(BATCH_SIZE).forEach { batch ->
                        em.transaction.begin()
                        batch.forEach { em.persist(it) }
                        em.transaction.commit()
                        em.clear()
                    }

                    log.info(
                        "[{}] 완료 - 플레이어: {}건, 집계: {}건 (기어: {}, 가젯: {}, 스타파워: {}, 하이퍼차지: {})",
                        JOB_NAME,
                        totalRead,
                        allEntities.size,
                        gearEntities.size,
                        gadgetEntities.size,
                        starPowerEntities.size,
                        hyperChargeEntities.size,
                    )
                }
                RepeatStatus.FINISHED
            }, txManager)
            .build()

    private data class BrawlerTrophyKey(
        val brawlerBrawlStarsId: Long,
        val trophyRange: BrawlerTrophyRange,
    )

    private data class BrawlerTrophyItemKey(
        val brawlerBrawlStarsId: Long,
        val trophyRange: BrawlerTrophyRange,
        val itemBrawlStarsId: Long,
    ) {
        fun toBrawlerTrophyKey() =
            BrawlerTrophyKey(
                brawlerBrawlStarsId = brawlerBrawlStarsId,
                trophyRange = trophyRange,
            )
    }
}
