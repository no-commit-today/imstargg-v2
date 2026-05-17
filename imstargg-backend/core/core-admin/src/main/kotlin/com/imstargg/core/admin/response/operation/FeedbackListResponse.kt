package com.imstargg.core.admin.response.operation

import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import org.springframework.data.domain.Page

data class FeedbackListResponse(
    val content: List<FeedbackResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<FeedbackEntity>): FeedbackListResponse =
            FeedbackListResponse(
                content = page.content.map(FeedbackResponse::from),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
