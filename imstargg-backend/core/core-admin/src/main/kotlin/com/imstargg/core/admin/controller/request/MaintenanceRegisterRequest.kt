package com.imstargg.core.admin.controller.request

import java.time.OffsetDateTime

data class MaintenanceRegisterRequest(
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
)
