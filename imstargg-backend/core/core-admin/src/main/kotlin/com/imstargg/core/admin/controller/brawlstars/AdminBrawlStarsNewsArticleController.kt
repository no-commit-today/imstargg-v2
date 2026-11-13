package com.imstargg.core.admin.controller.brawlstars

import com.imstargg.core.admin.application.brawlstars.AdminBrawlStarsNewsArticleService
import com.imstargg.core.admin.request.brawlstars.BrawlStarsNewsArticleTypeUpdateRequest
import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleDetailResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleListItemResponse
import com.imstargg.core.admin.response.brawlstars.BrawlStarsNewsArticleVersionResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminBrawlStarsNewsArticleController(
    private val service: AdminBrawlStarsNewsArticleService,
) {
    @GetMapping("/news-articles")
    fun list(): ResponseEntity<List<BrawlStarsNewsArticleListItemResponse>> = ResponseEntity.ok(service.findAll())

    @GetMapping("/news-articles/{id}")
    fun detail(
        @PathVariable id: Long,
    ): ResponseEntity<BrawlStarsNewsArticleDetailResponse> = ResponseEntity.ok(service.getDetail(id))

    @GetMapping("/news-articles/{id}/versions")
    fun versions(
        @PathVariable id: Long,
    ): ResponseEntity<List<BrawlStarsNewsArticleVersionResponse>> = ResponseEntity.ok(service.findVersions(id))

    @PutMapping("/news-articles/{id}/type")
    fun changeType(
        @PathVariable id: Long,
        @RequestBody request: BrawlStarsNewsArticleTypeUpdateRequest,
    ): ResponseEntity<Unit> {
        service.changeType(id = id, type = request.type)
        return ResponseEntity.noContent().build()
    }
}
