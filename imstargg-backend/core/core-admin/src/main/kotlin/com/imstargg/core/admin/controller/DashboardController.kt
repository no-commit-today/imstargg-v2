package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.DashboardService
import com.imstargg.core.admin.response.DashboardSummaryResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/dashboard/summary")
    fun getSummary(): ResponseEntity<DashboardSummaryResponse> = ResponseEntity.ok(dashboardService.getSummary())
}
