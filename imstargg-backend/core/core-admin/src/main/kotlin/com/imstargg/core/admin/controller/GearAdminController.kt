package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.GearAdminService
import com.imstargg.core.admin.controller.request.GearCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.GearAdminResponse
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
class GearAdminController(
    private val gearAdminService: GearAdminService,
) {
    @GetMapping("/gears")
    fun getAll(): ResponseEntity<List<GearAdminResponse>> {
        val gears = gearAdminService.findAll()
        return ResponseEntity.ok(gears.map { GearAdminResponse.from(it) })
    }

    @PostMapping("/gears")
    fun create(
        @RequestBody request: GearCreateRequest,
    ): ResponseEntity<Void> {
        gearAdminService.create(request.brawlStarsId, request.rarity)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        gearAdminService.updateMessages(MessageType.GEAR_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        gearAdminService.updateImage(
            BrawlStarsImageType.GEAR,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
