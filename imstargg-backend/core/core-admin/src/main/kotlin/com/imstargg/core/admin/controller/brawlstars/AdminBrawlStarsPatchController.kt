package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.core.admin.application.brawlstars.AdminBrawlStarsPatchService
import com.imstargg.core.admin.request.brawlstars.BrawlStarsPatchRegisterRequest
import com.imstargg.core.admin.request.brawlstars.BrawlStarsPatchUpdateRequest
import com.imstargg.core.admin.response.brawlstars.BrawlStarsPatchListResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsPatchResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminBrawlStarsPatchController(
    private val service: AdminBrawlStarsPatchService,
) {
    @GetMapping("/patches")
    fun list(): ResponseEntity<BrawlStarsPatchListResponse> = ResponseEntity.ok(service.findAll())

    @PostMapping("/patches")
    fun register(
        @RequestBody request: BrawlStarsPatchRegisterRequest,
    ): ResponseEntity<BrawlStarsPatchResponse> = ResponseEntity.ok(service.register(patchDate = request.patchDate, memo = request.memo))

    @PutMapping("/patches/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: BrawlStarsPatchUpdateRequest,
    ): ResponseEntity<Unit> {
        service.update(id = id, patchDate = request.patchDate, memo = request.memo)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/patches/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}
