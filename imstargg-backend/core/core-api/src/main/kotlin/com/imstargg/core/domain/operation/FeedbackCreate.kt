package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class FeedbackCreate(
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val ipHash: String,
)
