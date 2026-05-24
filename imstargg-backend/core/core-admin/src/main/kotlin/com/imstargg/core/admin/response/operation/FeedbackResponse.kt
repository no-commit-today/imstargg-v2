package com.imstargg.core.admin.response.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import java.time.Instant

data class FeedbackResponse(
    val id: Long,
    val rating: FeedbackRating,
    val content: String,
    val checked: Boolean,
    val submittedAt: Instant,
) {
    companion object {
        fun of(entity: FeedbackEntity): FeedbackResponse =
            FeedbackResponse(
                id = requireNotNull(entity.id),
                rating = entity.rating,
                content = entity.content,
                checked = entity.checked,
                submittedAt = entity.submittedAt,
            )
    }
}
