package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.BrawlerItemOwnershipStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamResultStatisticsItemResponse
import com.imstargg.core.api.query.StatisticsQueryComposer
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsDateRange
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
class StatisticsController(
    private val statisticsQueryComposer: StatisticsQueryComposer,
) {
    @GetMapping("/v1/statistics/brawler/result")
    fun getBrawlerResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
    ): ResponseEntity<BrawlerResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerResultStatistics(
                eventId?.let { BrawlStarsId(it) },
                tierRange,
                dateRange,
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/team/result")
    fun getBrawlerTeamResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam(required = false, defaultValue = "")
        @Size(max = 3)
        brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: ResultStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): ResponseEntity<ListResponse<BrawlerTeamResultStatisticsItemResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerTeamResultStatistics(
                eventId = eventId?.let { BrawlStarsId(it) },
                brawlerIds = brawlerIds.map { BrawlStarsId(it) },
                tierRange = tierRange,
                dateRange = dateRange,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/enemy/result")
    fun getEnemyResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: ResultStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): ResponseEntity<ListResponse<BrawlerEnemyResultStatisticsItemResponse>> {
        val responses =
            statisticsQueryComposer.getEnemyResultStatistics(
                eventId = eventId?.let { BrawlStarsId(it) },
                brawlerId = BrawlStarsId(brawlerId),
                tierRange = tierRange,
                dateRange = dateRange,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/rank")
    fun getBrawlerRankStatistics(
        @RequestParam eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
    ): ResponseEntity<ListResponse<BrawlerRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerRankStatistics(
                BrawlStarsId(eventId),
                tierRange,
                dateRange,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/team/rank")
    fun getBrawlerTeamRankStatistics(
        @RequestParam eventId: Long,
        @RequestParam(required = false, defaultValue = "")
        @Size(max = 3)
        brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: RankStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
    ): ResponseEntity<ListResponse<BrawlerTeamRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerTeamRankStatistics(
                eventId = BrawlStarsId(eventId),
                brawlerIds = brawlerIds.map { BrawlStarsId(it) },
                tierRange = tierRange,
                dateRange = dateRange,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/result/daily")
    fun getDailyResultStatistics(
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam dateRange: StatsDateRange,
    ): ResponseEntity<ListResponse<BrawlerResultDailyStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getDailyResultStatistics(
                BrawlStarsId(brawlerId),
                tierRange,
                dateRange,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/item/ownership")
    fun getBrawlerItemOwnershipStatistics(
        @RequestParam brawlerId: Long,
        @RequestParam trophyRange: BrawlerTrophyRange,
    ): ResponseEntity<BrawlerItemOwnershipStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerItemOwnershipStatistics(
                brawlerId = BrawlStarsId(brawlerId),
                trophyRange = trophyRange,
            )
        return ResponseEntity.ok(response)
    }
}
