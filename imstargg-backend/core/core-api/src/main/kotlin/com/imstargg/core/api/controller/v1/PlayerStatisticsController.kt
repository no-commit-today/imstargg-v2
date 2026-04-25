package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.v1.response.PlayerBrawlerRoleStatisticsResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerStatisticsResponse
import com.imstargg.core.api.query.PlayerStatisticsQueryComposer
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
class PlayerStatisticsController(
    private val playerStatisticsQueryComposer: PlayerStatisticsQueryComposer,
    private val clock: Clock,
) {
    @GetMapping("/v1/statistics/players/{tag}/brawler/ranked")
    fun getRankedBrawlerStatistics(
        @PathVariable tag: String,
        @RequestParam(required = false) seasonNumber: Int?,
    ): ResponseEntity<PlayerBrawlerStatisticsResponse> {
        val season = seasonNumber?.let { BrawlPassSeason.ofNumber(it) } ?: BrawlPassSeason.current(clock)
        val response = playerStatisticsQueryComposer.getRankedBrawlerStatistics(BrawlStarsTag(tag), season)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/players/{tag}/brawler/solo-ranked")
    fun getSoloRankedBrawlerStatistics(
        @PathVariable tag: String,
        @RequestParam(required = false) seasonKey: String?,
    ): ResponseEntity<PlayerBrawlerStatisticsResponse> {
        val season = seasonKey?.let { SoloRankSeason.ofKey(it) } ?: SoloRankSeason.current(clock)
        val response = playerStatisticsQueryComposer.getSoloRankedBrawlerStatistics(BrawlStarsTag(tag), season)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/players/{tag}/brawler-role/ranked")
    fun getRankedBrawlerRoleStatistics(
        @PathVariable tag: String,
        @RequestParam(required = false) seasonNumber: Int?,
    ): ResponseEntity<PlayerBrawlerRoleStatisticsResponse> {
        val season = seasonNumber?.let { BrawlPassSeason.ofNumber(it) } ?: BrawlPassSeason.current(clock)
        val response = playerStatisticsQueryComposer.getRankedBrawlerRoleStatistics(BrawlStarsTag(tag), season)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/statistics/players/{tag}/brawler-role/solo-ranked")
    fun getSoloRankedBrawlerRoleStatistics(
        @PathVariable tag: String,
        @RequestParam(required = false) seasonKey: String?,
    ): ResponseEntity<PlayerBrawlerRoleStatisticsResponse> {
        val season = seasonKey?.let { SoloRankSeason.ofKey(it) } ?: SoloRankSeason.current(clock)
        val response = playerStatisticsQueryComposer.getSoloRankedBrawlerRoleStatistics(BrawlStarsTag(tag), season)
        return ResponseEntity.ok(response)
    }
}
