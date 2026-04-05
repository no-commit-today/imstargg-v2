package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersResultStatisticsResponse
import com.imstargg.core.api.query.StatisticsQueryComposer
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
    private val statisticsQueryComposer: StatisticsQueryComposer,
) {
    @GetMapping("/v1/statistics/events/{eventId}/brawler/result")
    fun getBattleEventBrawlerResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBattleEventBrawlerResultStatistics(
                BattleEventBrawlerStatisticsParam(BrawlStarsId(eventId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/result/duo")
    fun getBattleEventBrawlerDuoResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlersResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBattleEventBrawlerDuoResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/result/trio")
    fun getBattleEventBrawlerTrioResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlersResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBattleEventBrawlerTrioResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/result/enemy")
    fun getBattleEventBrawlerEnemyResultStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerEnemyResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBattleEventBrawlerEnemyResultStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/rank")
    fun getBattleEventBrawlerRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBattleEventBrawlerRankStatistics(
                BattleEventBrawlerStatisticsParam(BrawlStarsId(eventId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/rank/duo")
    fun getBattleEventBrawlerDuoRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBattleEventBrawlerDuoRankStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/events/{eventId}/brawler/rank/trio")
    fun getBattleEventBrawlerTrioRankStatistics(
        @PathVariable eventId: Long,
        @RequestParam brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlersRankStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBattleEventBrawlerTrioRankStatistics(
                BattleEventBrawlersStatisticsParam(
                    BrawlStarsId(eventId),
                    BrawlStarsId(brawlerId),
                    tierRange,
                    startDate,
                    endDate,
                ),
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawler/result")
    fun getBrawlerResultStatistics(
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlerResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerResultStatistics(
                BrawlerStatisticsParam(tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawlers/{brawlerId}/result/daily")
    fun getBrawlerDailyResultStatistics(
        @PathVariable brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<ListResponse<BrawlerResultDailyStatisticsResponse>> {
        val responses =
            statisticsQueryComposer.getBrawlerDailyResultStatistics(
                BrawlersStatisticsParam(BrawlStarsId(brawlerId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(ListResponse(responses))
    }

    @GetMapping("/v1/statistics/brawlers/{brawlerId}/result/duo")
    fun getBrawlerDuoResultStatistics(
        @PathVariable brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlersResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerDuoResultStatistics(
                BrawlersStatisticsParam(BrawlStarsId(brawlerId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/brawlers/{brawlerId}/result/trio")
    fun getBrawlerTrioResultStatistics(
        @PathVariable brawlerId: Long,
        @RequestParam tierRange: StatsTierRange,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate,
    ): ResponseEntity<BrawlersResultStatisticsResponse> {
        val response =
            statisticsQueryComposer.getBrawlerTrioResultStatistics(
                BrawlersStatisticsParam(BrawlStarsId(brawlerId), tierRange, startDate, endDate),
            )
        return ResponseEntity.ok(response)
    }
}
