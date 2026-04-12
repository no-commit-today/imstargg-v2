package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerItemOwnershipStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerItemOwnershipStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamResultStatisticsItemResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.BattleRankStatisticsService
import com.imstargg.core.application.statistics.BattleResultStatisticsService
import com.imstargg.core.application.statistics.BrawlerItemOwnershipStatisticsService
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTeamSize
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

    fun getBrawlerTeamResultStatistics(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
        teamSize: StatsTeamSize,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
    ): List<BrawlerTeamResultStatisticsItemResponse> {
        val brawlers = brawlers()
        val statistics =
            battleResultStatisticsService.getBrawlerTeamResultStatistics(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                dateRange = dateRange,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.map { item ->
            BrawlerTeamResultStatisticsItemResponse(
                brawlers = item.brawlerIds.map { id -> brawlers.find(id)?.let { BrawlerResponse.of(it) } },
                totalBattleCount = item.totalBattleCount,
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
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
    ): List<BrawlerEnemyResultStatisticsItemResponse> {
        val brawlers = brawlers()
        val statistics =
            battleResultStatisticsService.getEnemyResultStatistics(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                dateRange = dateRange,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.map { item ->
            BrawlerEnemyResultStatisticsItemResponse(
                brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                enemyBrawler = brawlers.find(item.enemyBrawlerId)?.let { BrawlerResponse.of(it) },
                totalBattleCount = item.totalBattleCount,
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
        return battleResultStatisticsService
            .getDailyResultStatistics(brawlerId, tierRange, dateRange)
            .map { BrawlerResultDailyStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
    ): List<BrawlerRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleRankStatisticsService
            .getBrawlerRankStatistics(eventId, tierRange, dateRange)
            .map { BrawlerRankStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerTeamRankStatistics(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        dateRange: StatsDateRange,
        teamSize: StatsTeamSize,
        sortBy: RankStatsSortBy,
        sortOrder: SortOrder,
    ): List<BrawlerTeamRankStatisticsResponse> {
        val brawlers = brawlers()
        val statistics =
            battleRankStatisticsService.getBrawlerTeamRankStatistics(
                eventId = eventId,
                brawlerIds = brawlerIds,
                tierRange = tierRange,
                dateRange = dateRange,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.map { item ->
            BrawlerTeamRankStatisticsResponse(
                brawlers = item.brawlerIds.map { id -> brawlers.find(id)?.let { BrawlerResponse.of(it) } },
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

        return BrawlerItemOwnershipStatisticsResponse(
            gears = gears.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            gadgets = gadgets.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            starPowers = starPowers.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
            hyperCharges = hyperCharges.map { BrawlerItemOwnershipStatisticsItemResponse.of(it) },
        )
    }

    private fun brawlers(): Brawlers = Brawlers(brawlerService.getAll())

    private fun Brawlers.toBrawlerResponse(id: BrawlStarsId): BrawlerResponse? = find(id)?.let { BrawlerResponse.of(it) }

    companion object {
        private val DEFAULT_OFFSET_LIMIT = OffsetLimit(offset = 0, limit = 50)
    }
}
