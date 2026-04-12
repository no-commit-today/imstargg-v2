package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventService
import com.imstargg.core.admin.controller.request.BattleEventCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BattleEventResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleEventController(
    private val battleEventService: BattleEventService,
) {
    @GetMapping("/events")
    fun getAll(): ResponseEntity<List<BattleEventResponse>> = ResponseEntity.ok(battleEventService.findAllEvents())

    @PostMapping("/events")
    fun create(
        @RequestBody request: BattleEventCreateRequest,
    ): ResponseEntity<Unit> {
        battleEventService.createEvent(request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/events/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        battleEventService.updateEventMessages(brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }
}
