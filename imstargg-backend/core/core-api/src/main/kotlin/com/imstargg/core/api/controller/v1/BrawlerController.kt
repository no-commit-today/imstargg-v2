package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BrawlerController(
    private val brawlerService: BrawlerService,
) {
    @GetMapping("/v1/brawlers")
    fun getBrawlers(): ResponseEntity<ListResponse<BrawlerResponse>> {
        val brawlers = brawlerService.getAll()
        return ResponseEntity.ok(
            ListResponse(brawlers.map { BrawlerResponse.of(it) }),
        )
    }
}
