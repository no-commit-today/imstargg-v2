package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.GearService
import com.imstargg.core.admin.controller.support.toImageUpdate
import com.imstargg.core.admin.request.GearCreateRequest
import com.imstargg.core.admin.request.MessagesUpdateRequest
import com.imstargg.core.admin.response.GearResponse
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
class GearController(
    private val gearService: GearService,
) {
    @GetMapping("/gears")
    fun getAll(): ResponseEntity<List<GearResponse>> = ResponseEntity.ok(gearService.findAll())

    @PostMapping("/gears")
    fun create(
        @RequestBody request: GearCreateRequest,
    ): ResponseEntity<Unit> {
        gearService.create(request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        gearService.updateGearMessages(brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/gears/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        gearService.updateGearImage(brawlStarsId, file.toImageUpdate())
        return ResponseEntity.ok().build()
    }
}
