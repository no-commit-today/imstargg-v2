package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.enums.BuffyType

data class BuffyResponse(
    val type: BuffyType,
    val imageUrl: String?,
)
