package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlPassSeasonResponse
import com.imstargg.core.api.controller.v1.response.SoloRankSeasonResponse
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
class SeasonController(
    private val clock: Clock,
) {
    @GetMapping("/v1/seasons/brawl-pass")
    fun brawlPass(): ResponseEntity<ListResponse<BrawlPassSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(BrawlPassSeason.all(clock).map(BrawlPassSeasonResponse::of)),
        )

    @GetMapping("/v1/seasons/solo-rank")
    fun soloRank(): ResponseEntity<ListResponse<SoloRankSeasonResponse>> =
        ResponseEntity.ok(
            ListResponse(SoloRankSeason.all(clock).map(SoloRankSeasonResponse::of)),
        )
}
