package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.controller.request.BattleEventModeCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BattleEventModeAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
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
class BattleEventModeAdminController(
    private val battleEventAdminService: BattleEventAdminService,
) {
    @GetMapping("/event-modes")
    fun getAll(): ResponseEntity<List<BattleEventModeAdminResponse>> {
        val modes = battleEventAdminService.findAllModes()
        return ResponseEntity.ok(modes.map { BattleEventModeAdminResponse.from(it) })
    }

    @PostMapping("/event-modes")
    fun create(
        @RequestBody request: BattleEventModeCreateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.createMode(request.brawlStarsId, request.brawlStarsName)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateMessages(
            MessageType.BATTLE_EVENT_MODE_NAME,
            brawlStarsId,
            request.messages,
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/event-modes/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        battleEventAdminService.updateImage(
            BrawlStarsImageType.BATTLE_EVENT_MODE_ICON,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
