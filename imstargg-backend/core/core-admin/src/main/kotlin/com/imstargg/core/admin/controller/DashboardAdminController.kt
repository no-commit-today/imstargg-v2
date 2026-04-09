package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.DashboardService
import com.imstargg.core.admin.controller.response.DashboardSummaryAdminResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardAdminController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/dashboard/summary")
    fun getSummary(): ResponseEntity<DashboardSummaryAdminResponse> {
        val summary = dashboardService.getSummary()
        return ResponseEntity.ok(DashboardSummaryAdminResponse.from(summary))
    }
}
