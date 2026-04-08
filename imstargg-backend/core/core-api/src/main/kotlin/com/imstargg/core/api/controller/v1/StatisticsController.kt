package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerTeamResultStatisticsResponse
import com.imstargg.core.api.query.StatisticsQueryComposer
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.StatsTeamSize
import com.imstargg.core.enums.StatsTierRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @GetMapping("/v1/statistics/brawler/result/team")
    fun getBrawlerTeamResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam brawlerId: Long,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerTeamResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerTeamResultStatistics(
                eventId?.let { BrawlStarsId(it) },
                BrawlStarsId(brawlerId),
                tierRange,
                startDate,
                teamSize,
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawler/result/enemy")
    fun getEnemyResultStatistics(
        @RequestParam eventId: Long?,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerEnemyResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getEnemyResultStatistics(
                eventId?.let { BrawlStarsId(it) },
                BrawlStarsId(brawlerId),
                tierRange,
                startDate,
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/rank")
    fun getBrawlerRankStatistics(
        @PathVariable eventId: Long,
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

    @GetMapping("/v1/statistics/events/{eventId}/brawler/rank/team")
    fun getBrawlerTeamRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam teamSize: StatsTeamSize,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerTeamRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerTeamRankStatistics(
                BrawlStarsId(eventId),
                BrawlStarsId(brawlerId),
                tierRange,
                startDate,
                teamSize,
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawlers/{brawlerId}/result/daily")
    fun getDailyResultStatistics(
        @PathVariable brawlerId: Long,
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
