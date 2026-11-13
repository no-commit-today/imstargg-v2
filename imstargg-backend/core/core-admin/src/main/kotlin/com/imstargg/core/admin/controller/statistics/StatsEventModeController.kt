package com.imstargg.core.admin.controller.statistics

import com.imstargg.core.admin.application.statistics.StatsEventModeService
import com.imstargg.core.admin.request.statistics.StatsEventModeTypeUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class StatsEventModeController(
    private val statsEventModeService: StatsEventModeService,
) {
    @PutMapping("/event-modes/{brawlStarsId}/stats-type")
    fun updateType(
        @PathVariable brawlStarsId: Long,
        @RequestBody request: StatsEventModeTypeUpdateRequest,
    ): ResponseEntity<Unit> {
        statsEventModeService.updateType(brawlStarsId, request.type)
        return ResponseEntity.ok().build()
    }
}
