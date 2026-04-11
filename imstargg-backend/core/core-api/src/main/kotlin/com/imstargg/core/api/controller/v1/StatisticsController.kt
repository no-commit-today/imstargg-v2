package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamRankStatisticsListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamResultStatisticsResponse
import com.imstargg.core.api.query.StatisticsQueryComposer
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.RankStatsSortBy
import com.imstargg.core.enums.ResultStatsSortBy
import com.imstargg.core.enums.SortOrder
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class StatisticsController(
    private val statisticsQueryComposer: StatisticsQueryComposer,
) {
    @GetMapping("/v1/statistics/brawler/result")
    fun getBrawlerResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerResultStatistics(
                eventId?.let { BrawlStarsId(it) },
                tierRange,
                startDate,
                endDate,
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/team/result")
    fun getBrawlerTeamResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: ResultStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
        @ModelAttribute offsetLimitRequest: OffsetLimitRequest,
    ): ResponseEntity<BrawlerTeamResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerTeamResultStatistics(
                eventId = eventId?.let { BrawlStarsId(it) },
                brawlerIds = brawlerIds.map { BrawlStarsId(it) },
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimitRequest.toOffsetLimit(),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/enemy/result")
    fun getEnemyResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: ResultStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
        @ModelAttribute offsetLimitRequest: OffsetLimitRequest,
    ): ResponseEntity<BrawlerEnemyResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getEnemyResultStatistics(
                eventId = eventId?.let { BrawlStarsId(it) },
                brawlerId = BrawlStarsId(brawlerId),
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimitRequest.toOffsetLimit(),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/rank")
    fun getBrawlerRankStatistics(
        @RequestParam eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerRankStatistics(
                BrawlStarsId(eventId),
                tierRange,
                startDate,
                endDate,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/team/rank")
    fun getBrawlerTeamRankStatistics(
        @RequestParam eventId: Long,
        @RequestParam brawlerIds: List<Long>,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
        @RequestParam(defaultValue = "PICK_RATE") sortBy: RankStatsSortBy,
        @RequestParam(defaultValue = "DESC") sortOrder: SortOrder,
        @ModelAttribute offsetLimitRequest: OffsetLimitRequest,
    ): ResponseEntity<BrawlerTeamRankStatisticsListResponse> {
        val response =
            statisticsQueryComposer.getBrawlerTeamRankStatistics(
                eventId = BrawlStarsId(eventId),
                brawlerIds = brawlerIds.map { BrawlStarsId(it) },
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
                teamSize = teamSize,
                sortBy = sortBy,
                sortOrder = sortOrder,
                offsetLimit = offsetLimitRequest.toOffsetLimit(),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/result/daily")
    fun getDailyResultStatistics(
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerResultDailyStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getDailyResultStatistics(
                BrawlStarsId(brawlerId),
                tierRange,
                startDate,
                endDate,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }
}
