package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.api.controller.v1.response.BattleResponse
import com.imstargg.core.application.brawlstars.BattleService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleController(
    private val battleService: BattleService,
) {
    @GetMapping("/v1/players/{tag}/battles")
    fun getPlayerBattles(
        @PathVariable tag: String,
        @Valid @ModelAttribute request: OffsetLimitRequest,
    ): ResponseEntity<SliceResponse<BattleResponse>> {
        val battles =
            battleService.getBattles(
                BrawlStarsTag(tag),
                request.toOffsetLimit(),
            )
        return ResponseEntity.ok(
            SliceResponse.from(battles.map { BattleResponse.of(it) }),
        )
    }
}
