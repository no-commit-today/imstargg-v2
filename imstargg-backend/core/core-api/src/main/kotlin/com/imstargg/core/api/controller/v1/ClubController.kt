package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.api.controller.v1.response.ClubResponse
import com.imstargg.core.application.brawlstars.ClubService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
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

    @GetMapping("/v1/club/search")
    fun searchClubs(
        @RequestParam q: String,
        @ModelAttribute offsetLimit: OffsetLimitRequest,
    ): ResponseEntity<SliceResponse<ClubResponse>> {
        val slice = clubService.search(q, offsetLimit.toOffsetLimit())
        return ResponseEntity.ok(SliceResponse.from(slice.map { ClubResponse.of(it) }))
    }
}
