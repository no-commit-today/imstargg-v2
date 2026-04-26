package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventService
import com.imstargg.core.admin.controller.support.toImageUpdate
import com.imstargg.core.admin.request.BattleEventModeCreateRequest
import com.imstargg.core.admin.request.BattleEventModeResultTypeUpdateRequest
import com.imstargg.core.admin.request.MessagesUpdateRequest
import com.imstargg.core.admin.response.BattleEventModeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class BattleEventModeController(
    private val battleEventService: BattleEventService,
) {
    @GetMapping("/event-modes")
    fun getAll(): ResponseEntity<List<BattleEventModeResponse>> = ResponseEntity.ok(battleEventService.findAllModes())

    @PostMapping("/event-modes")
    fun create(
        @RequestBody request: BattleEventModeCreateRequest,
    ): ResponseEntity<Unit> {
        battleEventService.createMode(request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        battleEventService.updateModeMessages(brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        battleEventService.updateModeImage(brawlStarsId, file.toImageUpdate())
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/result-type")
    fun updateResultType(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BattleEventModeResultTypeUpdateRequest,
    ): ResponseEntity<Unit> {
        battleEventService.updateModeResultType(brawlStarsId, request.resultType)
        return ResponseEntity.ok().build()
    }
}
