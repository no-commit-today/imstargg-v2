package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankingResponse
import com.imstargg.core.api.controller.v1.response.ClubRankingResponse
import com.imstargg.core.api.controller.v1.response.PlayerRankingResponse
import com.imstargg.core.application.brawlstars.RankingService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.Country
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RankingController(
    private val rankingService: RankingService,
) {
    @GetMapping("/v1/ranking/players")
    fun getPlayerRankings(
        @RequestParam country: Country,
    ): ResponseEntity<ListResponse<PlayerRankingResponse>> {
        val rankings = rankingService.getPlayerRankings(country)
        return ResponseEntity.ok(
            ListResponse(rankings.map { PlayerRankingResponse.of(it) }),
        )
    }

    @GetMapping("/v1/ranking/brawlers/{brawlerId}")
    fun getBrawlerRankings(
        @PathVariable brawlerId: Long,
        @RequestParam country: Country,
    ): ResponseEntity<ListResponse<BrawlerRankingResponse>> {
        val rankings = rankingService.getBrawlerRankings(country, BrawlStarsId(brawlerId))
        return ResponseEntity.ok(
            ListResponse(rankings.map { BrawlerRankingResponse.of(it) }),
        )
    }

    @GetMapping("/v1/ranking/clubs")
    fun getClubRankings(
        @RequestParam country: Country,
    ): ResponseEntity<ListResponse<ClubRankingResponse>> {
        val rankings = rankingService.getClubRankings(country)
        return ResponseEntity.ok(
            ListResponse(rankings.map { ClubRankingResponse.of(it) }),
        )
    }
}
