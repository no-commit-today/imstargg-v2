package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BattleEventResponse
import com.imstargg.core.api.controller.v1.response.RotationBattleEventResponse
import com.imstargg.core.application.brawlstars.BattleEventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleEventController(
    private val battleEventService: BattleEventService,
) {
    @GetMapping("/api/v1/events/rotation")
    fun getRotationEvents(): ResponseEntity<ListResponse<RotationBattleEventResponse>> {
        val events = battleEventService.getCurrentRotationEvents()
        return ResponseEntity.ok(
            ListResponse(events.map { RotationBattleEventResponse.of(it) }),
        )
    }

    @GetMapping("/api/v1/events/solo-rank")
    fun getSoloRankEvents(): ResponseEntity<ListResponse<BattleEventResponse>> {
        val events = battleEventService.getSoloRankEvents()
        return ResponseEntity.ok(
            ListResponse(events.map { BattleEventResponse.of(it) }),
        )
    }
}
