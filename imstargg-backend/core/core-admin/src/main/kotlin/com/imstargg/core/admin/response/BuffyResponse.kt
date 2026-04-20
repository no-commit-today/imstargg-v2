package com.imstargg.core.admin.response

import com.imstargg.core.enums.BuffyType

data class BuffyResponse(
    val type: BuffyType,
    val imageUrl: String?,
)
