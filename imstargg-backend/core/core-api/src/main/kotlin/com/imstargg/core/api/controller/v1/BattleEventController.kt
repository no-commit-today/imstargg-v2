package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BattleEventResponse
import com.imstargg.core.api.controller.v1.response.RotationBattleEventResponse
import com.imstargg.core.application.brawlstars.BattleEventService
import com.imstargg.core.application.statistics.StatsEventModeService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleEventController(
    private val battleEventService: BattleEventService,
    private val statsEventModeService: StatsEventModeService,
) {
    @GetMapping("/v1/events/rotation")
    fun getRotationEvents(): ResponseEntity<ListResponse<RotationBattleEventResponse>> {
        val events = battleEventService.getCurrentRotationEvents()
        val statsEventModeTypes = statsEventModeService.getTypes()
        return ResponseEntity.ok(
            ListResponse(events.map { RotationBattleEventResponse.of(it, statsEventModeTypes) }),
        )
    }

    @GetMapping("/v1/events/{id}")
    fun getEvent(
        @PathVariable id: Long,
    ): ResponseEntity<BattleEventResponse> {
        val event = battleEventService.get(BrawlStarsId(id))
        return ResponseEntity.ok(BattleEventResponse.of(event, statsEventModeService.getTypes()))
    }

    @GetMapping("/v1/events/solo-rank")
    fun getSoloRankEvents(
        @RequestParam(required = false) seasonKey: String?,
    ): ResponseEntity<ListResponse<BattleEventResponse>> {
        val events = battleEventService.getSoloRankEvents(seasonKey)
        val statsEventModeTypes = statsEventModeService.getTypes()
        return ResponseEntity.ok(
            ListResponse(events.map { BattleEventResponse.of(it, statsEventModeTypes) }),
        )
    }

    @GetMapping("/v1/event/search")
    fun searchEvents(
        @RequestParam name: String,
    ): ResponseEntity<ListResponse<BattleEventResponse>> {
        val events = battleEventService.search(name)
        val statsEventModeTypes = statsEventModeService.getTypes()
        return ResponseEntity.ok(
            ListResponse(events.map { BattleEventResponse.of(it, statsEventModeTypes) }),
        )
    }
}
