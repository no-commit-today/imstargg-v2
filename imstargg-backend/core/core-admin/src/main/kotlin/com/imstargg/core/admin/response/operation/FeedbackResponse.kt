package com.imstargg.core.admin.response.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import java.time.Instant

data class FeedbackResponse(
    val id: Long,
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val ipHash: String,
    val checked: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun of(entity: FeedbackEntity): FeedbackResponse =
            FeedbackResponse(
                id = requireNotNull(entity.id),
                category = entity.category,
                content = entity.content,
                email = entity.email,
                ipHash = entity.ipHash,
                checked = entity.checked,
                createdAt = requireNotNull(entity.createdAt),
            )
    }
}
