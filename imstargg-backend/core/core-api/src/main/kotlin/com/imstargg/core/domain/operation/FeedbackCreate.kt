package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackRating
import java.time.Instant

data class FeedbackCreate(
    val rating: FeedbackRating,
    val content: String,
    val submittedAt: Instant,
)
