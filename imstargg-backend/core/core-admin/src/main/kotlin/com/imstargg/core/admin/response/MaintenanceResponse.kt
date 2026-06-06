package com.imstargg.core.admin.response

import java.time.Instant

data class MaintenanceResponse(
    val id: Long,
    val startTime: Instant,
    val endTime: Instant,
    val createdAt: Instant?,
)
