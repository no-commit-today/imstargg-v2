package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerBuffyOwnershipStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerItemOwnershipStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerItemOwnershipStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerPairRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerPairResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.BattleRankStatisticsService
import com.imstargg.core.application.statistics.BattleResultStatisticsService
import com.imstargg.core.application.statistics.BrawlerItemOwnershipStatisticsService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Component

@Component
class StatisticsQueryComposer(
    private val brawlerService: BrawlerService,
    private val battleResultStatisticsService: BattleResultStatisticsService,
    private val battleRankStatisticsService: BattleRankStatisticsService,
    private val brawlerItemOwnershipStatisticsService: BrawlerItemOwnershipStatisticsService,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): BrawlerResultStatisticsResponse {
        val brawlers = brawlers()
        return BrawlerResultStatisticsResponse.of(
            battleResultStatisticsService.getBrawlerResultStatistics(eventId, tierRange, dateRange),
            brawlers,
        )
    }

    fun getBrawlerPairResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerPairResultStatisticsItemResponse> {
        val brawlers = brawlers()
        val statistics =
            battleResultStatisticsService.getBrawlerPairResultStatistics(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                dateRange = dateRange,
            )
        return statistics.items.map { item ->
            BrawlerPairResultStatisticsItemResponse(
                brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                pairBrawler = brawlers.find(item.pairBrawlerId)?.let { BrawlerResponse.of(it) },
                battleCount = item.battleCount,
                pickRate = item.pickRate,
                winRate = item.winRate,
            )
        }
    }

    fun getEnemyResultStatistics(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerEnemyResultStatisticsItemResponse> {
        val brawlers = brawlers()
        val statistics =
            battleResultStatisticsService.getEnemyResultStatistics(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                dateRange = dateRange,
            )
        return statistics.items.map { item ->
            BrawlerEnemyResultStatisticsItemResponse(
                brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                enemyBrawler = brawlers.find(item.enemyBrawlerId)?.let { BrawlerResponse.of(it) },
                battleCount = item.battleCount,
                pickRate = item.pickRate,
                winRate = item.winRate,
            )
        }
    }

    fun getDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerResultDailyStatisticsResponse> {
        val brawlers = brawlers()
        val statistics = battleResultStatisticsService.getDailyResultStatistics(brawlerId, tierRange, dateRange)
        val brawlerResponse = brawlers.toBrawlerResponse(statistics.brawlerId)
        return statistics.items.map { BrawlerResultDailyStatisticsResponse.of(it, brawlerResponse) }
    }

    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleRankStatisticsService
            .getBrawlerRankStatistics(eventId, tierRange, dateRange)
            .items
            .map { BrawlerRankStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerPairRankStatisticsResponse> {
        val brawlers = brawlers()
        val statistics =
            battleRankStatisticsService.getBrawlerPairRankStatistics(
                eventId = eventId,
                brawlerId = brawlerId,
                pairBrawlerId = pairBrawlerId,
                tierRange = tierRange,
                dateRange = dateRange,
            )
        return statistics.items.map { item ->
            BrawlerPairRankStatisticsResponse(
                brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                pairBrawler = brawlers.find(item.pairBrawlerId)?.let { BrawlerResponse.of(it) },
                totalBattleCount = item.totalBattleCount,
                averageRank = item.averageRank,
            )
        }
    }

    fun getBrawlerItemOwnershipStatistics(
        brawlerId: BrawlStarsId,
        trophyRange: BrawlerTrophyRange,
    ): BrawlerItemOwnershipStatisticsResponse {
        val gears = brawlerItemOwnershipStatisticsService.getGearOwnership(brawlerId, trophyRange)
        val gadgets = brawlerItemOwnershipStatisticsService.getGadgetOwnership(brawlerId, trophyRange)
        val starPowers = brawlerItemOwnershipStatisticsService.getStarPowerOwnership(brawlerId, trophyRange)
        val hyperCharges = brawlerItemOwnershipStatisticsService.getHyperChargeOwnership(brawlerId, trophyRange)
        val buffies = brawlerItemOwnershipStatisticsService.getBuffyOwnership(brawlerId, trophyRange)

        return BrawlerItemOwnershipStatisticsResponse(
            gears = gears.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            gadgets = gadgets.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            starPowers = starPowers.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            hyperCharges = hyperCharges.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            buffies = buffies.map { BrawlerBuffyOwnershipStatisticsItemResponse.of(it) },
        )
    }

    private fun brawlers(): Brawlers = Brawlers(brawlerService.getAll())

    private fun Brawlers.toBrawlerResponse(id: BrawlStarsId): BrawlerResponse? = find(id)?.let { BrawlerResponse.of(it) }
}
