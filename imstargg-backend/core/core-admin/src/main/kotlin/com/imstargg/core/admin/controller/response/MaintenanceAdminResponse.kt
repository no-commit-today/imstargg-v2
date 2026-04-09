package com.imstargg.core.admin.controller.response

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import java.time.Instant
import java.time.OffsetDateTime

data class MaintenanceAdminResponse(
    val id: Long,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val createdAt: Instant?,
) {
    companion object {
        fun from(entity: BrawlStarsApiMaintenanceEntity) =
            MaintenanceAdminResponse(
                id = entity.id!!,
                startTime = entity.startTime,
                endTime = entity.endTime,
                createdAt = entity.createdAt,
            )
    }
}
