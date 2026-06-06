package com.imstargg.core.admin.request

import java.time.Instant

data class MaintenanceRegisterRequest(
    val startTime: Instant,
    val endTime: Instant,
)
