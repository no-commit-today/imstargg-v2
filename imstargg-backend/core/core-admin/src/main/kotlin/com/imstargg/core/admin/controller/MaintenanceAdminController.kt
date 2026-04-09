package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlStarsApiMaintenanceAdminService
import com.imstargg.core.admin.controller.request.MaintenanceRegisterRequest
import com.imstargg.core.admin.controller.response.MaintenanceAdminResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MaintenanceAdminController(
    private val maintenanceAdminService: BrawlStarsApiMaintenanceAdminService,
) {
    @GetMapping("/maintenances")
    fun getAll(): ResponseEntity<List<MaintenanceAdminResponse>> {
        val maintenances = maintenanceAdminService.findAll()
        return ResponseEntity.ok(maintenances.map { MaintenanceAdminResponse.from(it) })
    }

    @PostMapping("/maintenances")
    fun register(
        @RequestBody request: MaintenanceRegisterRequest,
    ): ResponseEntity<MaintenanceAdminResponse> {
        val maintenance = maintenanceAdminService.register(request.startTime, request.endTime)
        return ResponseEntity.ok(MaintenanceAdminResponse.from(maintenance))
    }

    @DeleteMapping("/maintenances/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        maintenanceAdminService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
