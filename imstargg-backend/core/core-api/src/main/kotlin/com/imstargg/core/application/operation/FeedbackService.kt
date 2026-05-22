package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackRating
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class FeedbackService(
    private val repository: FeedbackRepository,
    private val clock: Clock,
) {
    fun submit(
        rating: FeedbackRating,
        content: String,
    ): Feedback {
        val command =
            FeedbackCreate(
                rating = rating,
                content = content.trim(),
                submittedAt = clock.instant(),
            )
        return repository.save(command)
    }
}
