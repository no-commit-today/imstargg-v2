package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.core.admin.application.brawlstars.AdminBrawlStarsApiSpecService
import com.imstargg.core.admin.response.brawlstars.BrawlStarsApiSpecDetailResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsApiSpecListItemResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminBrawlStarsApiSpecController(
    private val service: AdminBrawlStarsApiSpecService,
) {
    @GetMapping("/api-specs")
    fun list(): ResponseEntity<List<BrawlStarsApiSpecListItemResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/api-specs/{id}")
    fun detail(
        @PathVariable id: Long,
    ): ResponseEntity<BrawlStarsApiSpecDetailResponse> = ResponseEntity.ok(service.getDetail(id))
}
