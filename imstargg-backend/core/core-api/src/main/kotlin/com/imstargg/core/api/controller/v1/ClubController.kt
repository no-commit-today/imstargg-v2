package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.v1.response.ClubResponse
import com.imstargg.core.application.brawlstars.ClubService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ClubController(
    private val clubService: ClubService,
) {
    @GetMapping("/v1/clubs/{tag}")
    fun getClub(
        @PathVariable tag: String,
    ): ResponseEntity<ClubResponse> {
        val club = clubService.get(BrawlStarsTag(tag))
        return ResponseEntity.ok(ClubResponse.of(club))
    }
}
