package com.imstargg.core.admin.application

import com.imstargg.core.admin.request.MaintenanceRegisterRequest
import com.imstargg.core.admin.response.MaintenanceResponse
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BrawlStarsApiMaintenanceService(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
) {
    fun findAll(): List<MaintenanceResponse> =
        brawlStarsApiMaintenanceJpaRepository.findAll().map { entity ->
            MaintenanceResponse(
                id = entity.id!!,
                startTime = entity.startTime,
                endTime = entity.endTime,
                createdAt = entity.createdAt,
            )
        }

    @Transactional
    fun register(request: MaintenanceRegisterRequest): MaintenanceResponse {
        val entity =
            brawlStarsApiMaintenanceJpaRepository.save(
                BrawlStarsApiMaintenanceEntity(
                    startTime = request.startTime,
                    endTime = request.endTime,
                ),
            )
        return MaintenanceResponse(
            id = entity.id!!,
            startTime = entity.startTime,
            endTime = entity.endTime,
            createdAt = entity.createdAt,
        )
    }

    @Transactional
    fun delete(id: Long) {
        brawlStarsApiMaintenanceJpaRepository.deleteById(id)
    }
}
