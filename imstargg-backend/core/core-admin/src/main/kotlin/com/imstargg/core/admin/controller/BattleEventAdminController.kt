package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.controller.request.BattleEventCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BattleEventAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BattleEventAdminController(
    private val battleEventAdminService: BattleEventAdminService,
) {
    @GetMapping("/events")
    fun getAll(): ResponseEntity<List<BattleEventAdminResponse>> {
        val events = battleEventAdminService.findAllEvents()
        return ResponseEntity.ok(events.map { BattleEventAdminResponse.from(it) })
    }

    @PostMapping("/events")
    fun create(
        @RequestBody request: BattleEventCreateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.createEvent(request.brawlStarsId, request.modeId, request.mapName)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/events/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateMessages(MessageType.BATTLE_EVENT_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }
}
