package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerResponse
import com.imstargg.core.api.controller.v1.response.PlayerResponse
import com.imstargg.core.application.brawlstars.PlayerService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlayerController(
    private val playerService: PlayerService,
) {
    @GetMapping("/v1/players/{tag}")
    fun getPlayer(
        @PathVariable tag: String,
    ): ResponseEntity<PlayerResponse> {
        val player = playerService.get(BrawlStarsTag(tag))
        return ResponseEntity.ok(PlayerResponse.of(player))
    }

    @GetMapping("/v1/players/{tag}/brawlers")
    fun getPlayerBrawlers(
        @PathVariable tag: String,
    ): ResponseEntity<ListResponse<PlayerBrawlerResponse>> {
        val playerBrawlers = playerService.getPlayerBrawlers(BrawlStarsTag(tag))
        return ResponseEntity.ok(
            ListResponse(playerBrawlers.map { PlayerBrawlerResponse.of(it) }),
        )
    }

    @GetMapping("/v1/player/search")
    fun searchPlayers(
        @RequestParam q: String,
        @ModelAttribute offsetLimit: OffsetLimitRequest,
    ): ResponseEntity<SliceResponse<PlayerResponse>> {
        val slice = playerService.search(q, offsetLimit.toOffsetLimit())
        return ResponseEntity.ok(SliceResponse.from(slice.map { PlayerResponse.of(it) }))
    }
}
