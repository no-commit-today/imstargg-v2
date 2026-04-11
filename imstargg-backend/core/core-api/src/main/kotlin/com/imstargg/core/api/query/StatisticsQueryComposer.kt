package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamResultStatisticsItemResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.BattleRankStatisticsService
import com.imstargg.core.application.statistics.BattleResultStatisticsService
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class StatisticsQueryComposer(
    private val brawlerService: BrawlerService,
    private val battleResultStatisticsService: BattleResultStatisticsService,
    private val battleRankStatisticsService: BattleRankStatisticsService,
) {
    fun getBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatisticsResponse {
        val brawlers = brawlers()
        return BrawlerResultStatisticsResponse.of(
            battleResultStatisticsService.getBrawlerResultStatistics(eventId, tierRange, startDate, endDate),
            brawlers,
        )
    }

    fun getBrawlerTeamResultStatistics(
        eventId: BrawlStarsId?,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
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
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.content.map { item ->
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
        startDate: LocalDate,
        endDate: LocalDate,
        sortBy: ResultStatsSortBy,
        sortOrder: SortOrder,
    ): List<BrawlerEnemyResultStatisticsItemResponse> {
        val brawlers = brawlers()
        val statistics =
            battleResultStatisticsService.getEnemyResultStatistics(
                eventId = eventId,
                brawlerId = brawlerId,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.content.map { item ->
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
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerResultDailyStatisticsResponse> {
        val brawlers = brawlers()
        return battleResultStatisticsService
            .getDailyResultStatistics(brawlerId, tierRange, startDate, endDate)
            .map { BrawlerResultDailyStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleRankStatisticsService
            .getBrawlerRankStatistics(eventId, tierRange, startDate, endDate)
            .map { BrawlerRankStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerTeamRankStatistics(
        eventId: BrawlStarsId,
        brawlerIds: List<BrawlStarsId>,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
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
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = DEFAULT_OFFSET_LIMIT,
            )
        return statistics.items.content.map { item ->
            BrawlerTeamRankStatisticsResponse(
                brawlers = item.brawlerIds.map { id -> brawlers.find(id)?.let { BrawlerResponse.of(it) } },
                totalBattleCount = item.totalBattleCount,
                averageRank = item.averageRank,
            )
        }
    }

    private fun brawlers(): Brawlers = Brawlers(brawlerService.getAll())

    private fun Brawlers.toBrawlerResponse(id: BrawlStarsId): BrawlerResponse? = find(id)?.let { BrawlerResponse.of(it) }

    companion object {
        private val DEFAULT_OFFSET_LIMIT = OffsetLimit(offset = 0, limit = 50)
    }
}
