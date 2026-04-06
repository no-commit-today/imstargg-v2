package com.imstargg.core.admin.application

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
@Transactional(readOnly = true)
class BrawlStarsApiMaintenanceAdminService(
    private val brawlStarsApiMaintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
) {
    fun findAll(): List<BrawlStarsApiMaintenanceEntity> = brawlStarsApiMaintenanceJpaRepository.findAll()

    @Transactional
    fun register(
        startTime: OffsetDateTime,
        endTime: OffsetDateTime,
    ): BrawlStarsApiMaintenanceEntity =
        brawlStarsApiMaintenanceJpaRepository.save(
            BrawlStarsApiMaintenanceEntity(startTime = startTime, endTime = endTime),
        )

    @Transactional
    fun delete(id: Long) {
        brawlStarsApiMaintenanceJpaRepository.deleteById(id)
    }
}
