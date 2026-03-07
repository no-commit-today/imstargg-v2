package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersResultStatisticsResponse
import com.imstargg.core.application.statistics.BattleEventStatisticsService
import com.imstargg.core.application.statistics.BrawlerStatisticsService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BattleEventBrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BattleEventBrawlersStatisticsParam
import com.imstargg.core.domain.statistics.BrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BrawlersStatisticsParam
import com.imstargg.core.enums.StatsTierRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class StatisticsController(
    private val battleEventStatisticsService: BattleEventStatisticsService,
    private val brawlerStatisticsService: BrawlerStatisticsService,
) {
    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/result")
    fun getBattleEventBrawlerResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerResultStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerResultStatistics(
                BattleEventBrawlerStatisticsParam(BrawlStarsId(eventId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlerResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/result/duo")
    fun getBattleEventBrawlerDuoResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersResultStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerDuoResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/result/trio")
    fun getBattleEventBrawlerTrioResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersResultStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerTrioResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/result/enemy")
    fun getBattleEventBrawlerEnemyResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerEnemyResultStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerEnemyResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlerEnemyResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/rank")
    fun getBattleEventBrawlerRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerRankStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerRankStatistics(
                BattleEventBrawlerStatisticsParam(BrawlStarsId(eventId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlerRankStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/rank/duo")
    fun getBattleEventBrawlerDuoRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersRankStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerDuoRankStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersRankStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/events/{eventId}/brawler/rank/trio")
    fun getBattleEventBrawlerTrioRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersRankStatisticsResponse>> {
        val statistics =
            battleEventStatisticsService.getBattleEventBrawlerTrioRankStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersRankStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/brawler/result")
    fun getBrawlerResultStatistics(
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerResultStatisticsResponse>> {
        val statistics =
            brawlerStatisticsService.getBrawlerResultStatistics(
                BrawlerStatisticsParam(tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlerResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/brawlers/{brawlerId}/result/duo")
    fun getBrawlerDuoResultStatistics(
        @PathVariable brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersResultStatisticsResponse>> {
        val statistics =
            brawlerStatisticsService.getBrawlerDuoResultStatistics(
                BrawlersStatisticsParam(BrawlStarsId(brawlerId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersResultStatisticsResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/statistics/brawlers/{brawlerId}/result/trio")
    fun getBrawlerTrioResultStatistics(
        @PathVariable brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersResultStatisticsResponse>> {
        val statistics =
            brawlerStatisticsService.getBrawlerTrioResultStatistics(
                BrawlersStatisticsParam(BrawlStarsId(brawlerId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(
            ListResponse(statistics.map { BrawlersResultStatisticsResponse.of(it) }),
        )
    }
}
