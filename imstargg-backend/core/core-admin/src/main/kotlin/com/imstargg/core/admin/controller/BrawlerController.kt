package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlerService
import com.imstargg.core.admin.controller.request.BrawlerCreateRequest
import com.imstargg.core.admin.controller.request.BrawlerGearsUpdateRequest
import com.imstargg.core.admin.controller.request.BrawlerSubItemCreateRequest
import com.imstargg.core.admin.controller.request.MessagesUpdateRequest
import com.imstargg.core.admin.controller.response.BrawlerDetailResponse
import com.imstargg.core.admin.controller.response.BrawlerResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
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
class BrawlerController(
    private val brawlerService: BrawlerService,
) {
    @GetMapping("/brawlers")
    fun getAll(): ResponseEntity<List<BrawlerResponse>> = ResponseEntity.ok(brawlerService.findAll())

    @GetMapping("/brawlers/{brawlStarsId}")
    fun getDetail(
        @PathVariable brawlStarsId: Long,
    ): ResponseEntity<BrawlerDetailResponse> {
        val brawler =
            brawlerService.findDetail(brawlStarsId)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(brawler)
    }

    @PostMapping("/brawlers")
    fun create(
        @RequestBody request: BrawlerCreateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.createBrawler(request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/messages")
    fun updateMessages(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.updateBrawlerMessages(brawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/image")
    fun updateImage(
        @PathVariable brawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        brawlerService.updateBrawlerImage(
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
    ): ResponseEntity<Unit> {
        brawlerService.updateBuffyImage(
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
    ): ResponseEntity<Unit> {
        brawlerService.updateBrawlerGears(brawlStarsId, request.gearBrawlStarsIds)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/brawlers/{brawlStarsId}/star-powers")
    fun createStarPower(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: BrawlerSubItemCreateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.createStarPower(brawlStarsId, request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/messages")
    fun updateStarPowerMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.updateStarPowerMessages(starPowerBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/star-powers/{starPowerBrawlStarsId}/image")
    fun updateStarPowerImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable starPowerBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        brawlerService.updateStarPowerImage(
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
    ): ResponseEntity<Unit> {
        brawlerService.createGadget(brawlStarsId, request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/messages")
    fun updateGadgetMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.updateGadgetMessages(gadgetBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/gadgets/{gadgetBrawlStarsId}/image")
    fun updateGadgetImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable gadgetBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        brawlerService.updateGadgetImage(
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
    ): ResponseEntity<Unit> {
        brawlerService.createHyperCharge(brawlStarsId, request)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/messages")
    fun updateHyperChargeMessages(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestBody request: MessagesUpdateRequest,
    ): ResponseEntity<Unit> {
        brawlerService.updateHyperChargeMessages(hyperChargeBrawlStarsId, request.messages)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/brawlers/{brawlStarsId}/hyper-charges/{hyperChargeBrawlStarsId}/image")
    fun updateHyperChargeImage(
        @PathVariable brawlStarsId: Long,
        @PathVariable hyperChargeBrawlStarsId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<Unit> {
        brawlerService.updateHyperChargeImage(
            hyperChargeBrawlStarsId,
            file.inputStream,
            file.contentType ?: "image/png",
        )
        return ResponseEntity.ok().build()
    }
}
