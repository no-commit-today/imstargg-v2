package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackCategory
import org.springframework.stereotype.Service

@Service
class FeedbackService(
    private val repository: FeedbackRepository,
) {
    fun submit(
        category: FeedbackCategory,
        content: String,
        email: String?,
    ): Feedback {
        val command =
            FeedbackCreate(
                category = category,
                content = content.trim(),
                email = email?.takeIf { it.isNotBlank() },
            )
        return repository.save(command)
    }
}
