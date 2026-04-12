package com.imstargg.core.admin.controller

import com.imstargg.core.admin.application.BrawlStarsApiMaintenanceService
import com.imstargg.core.admin.controller.request.MaintenanceRegisterRequest
import com.imstargg.core.admin.controller.response.MaintenanceResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class MaintenanceController(
    private val maintenanceService: BrawlStarsApiMaintenanceService,
) {
    @GetMapping("/maintenances")
    fun getAll(): ResponseEntity<List<MaintenanceResponse>> = ResponseEntity.ok(maintenanceService.findAll())

    @PostMapping("/maintenances")
    fun register(
        @RequestBody request: MaintenanceRegisterRequest,
    ): ResponseEntity<MaintenanceResponse> = ResponseEntity.ok(maintenanceService.register(request))

    @DeleteMapping("/maintenances/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        maintenanceService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
