package com.imstargg.core.admin.controller.response

import java.time.Instant
import java.time.OffsetDateTime

data class MaintenanceResponse(
    val id: Long,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val createdAt: Instant?,
)
