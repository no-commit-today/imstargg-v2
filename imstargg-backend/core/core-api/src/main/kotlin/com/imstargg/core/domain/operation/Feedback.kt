package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class Feedback(
    val id: Long,
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val checked: Boolean,
)
