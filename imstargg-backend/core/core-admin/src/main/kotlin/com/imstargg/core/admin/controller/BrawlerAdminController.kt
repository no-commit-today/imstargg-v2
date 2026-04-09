package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlerAdminService
import com.imstargg.core.admin.controller.request.BrawlerCreateRequest
import com.imstargg.core.admin.controller.request.BrawlerGearsUpdateRequest
import com.imstargg.core.admin.controller.request.BrawlerSubItemCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BrawlerAdminResponse
import com.imstargg.core.admin.controller.response.BrawlerDetailAdminResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class BrawlerAdminController(
    private val brawlerAdminService: BrawlerAdminService,
) {
    @GetMapping("/brawlers")
    fun getAll(): ResponseEntity<List<BrawlerAdminResponse>> {
        val brawlers = brawlerAdminService.findAll()
        return ResponseEntity.ok(brawlers.map { BrawlerAdminResponse.from(it) })
    }

    @GetMapping("/brawlers/{brawlStarsId}")
    fun getDetail(
        @PathVariable brawlStarsId: Long,
    ): ResponseEntity<BrawlerDetailAdminResponse> {
        val brawler =
            brawlerAdminService.findDetail(brawlStarsId)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(BrawlerDetailAdminResponse.from(brawler))
    }

    @PostMapping("/brawlers")
    fun create(
        @RequestBody request: BrawlerCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createBrawler(request.brawlStarsId, request.rarity, request.role)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.BRAWLER_NAME, brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.BRAWLER_PORTRAIT,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/buffy-image")
    fun updateBuffyImage(
        @PathVariable brawlStarsId: Long,
        @RequestParam buffyType: BrawlStarsImageType,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            buffyType,
            brawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gears")
    fun updateGears(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerGearsUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateBrawlerGears(brawlStarsId, request.gearBrawlStarsIds)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/brawlers/{brawlStarsId}/star-powers")
    fun createStarPower(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createStarPower(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/messages")
    fun updateStarPowerMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.STARPOWER_NAME, starPowerBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/image")
    fun updateStarPowerImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.STARPOWER,
            starPowerBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/brawlers/{brawlStarsId}/gadgets")
    fun createGadget(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createGadget(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/messages")
    fun updateGadgetMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.GADGET_NAME, gadgetBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/image")
    fun updateGadgetImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.GADGET,
            gadgetBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/brawlers/{brawlStarsId}/hyper-charges")
    fun createHyperCharge(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.createHyperCharge(request.brawlStarsId, brawlStarsId)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/messages")
    fun updateHyperChargeMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateMessages(MessageType.HYPERCHARGE_NAME, hyperChargeBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/image")
    fun updateHyperChargeImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Void> {
        brawlerAdminService.updateImage(
            BrawlStarsImageType.HYPERCHARGE,
            hyperChargeBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
