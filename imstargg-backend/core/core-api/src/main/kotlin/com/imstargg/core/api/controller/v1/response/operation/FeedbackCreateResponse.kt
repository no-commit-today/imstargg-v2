package com.imstargg.core.api.controller.v1.response.operation

import com.imstargg.core.domain.operation.Feedback

data class FeedbackCreateResponse(
    val id: Long,
) {
    companion object {
        fun of(feedback: Feedback): FeedbackCreateResponse = FeedbackCreateResponse(id = feedback.id)
    }
}
