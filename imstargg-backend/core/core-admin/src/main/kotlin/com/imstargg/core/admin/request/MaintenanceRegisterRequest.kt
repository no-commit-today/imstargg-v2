package com.imstargg.core.admin.request

import java.time.OffsetDateTime

data class MaintenanceRegisterRequest(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
