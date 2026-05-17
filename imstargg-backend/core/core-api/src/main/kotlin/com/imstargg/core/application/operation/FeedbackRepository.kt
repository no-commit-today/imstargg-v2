package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.springframework.stereotype.Repository

@Repository
class FeedbackRepository(
    private val jpaRepository: FeedbackJpaRepository,
) {
    fun save(command: FeedbackCreate): Feedback {
        val entity =
            FeedbackEntity(
                category = command.category,
                content = command.content,
                email = command.email,
                ipHash = command.ipHash,
            )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    private fun FeedbackEntity.toDomain(): Feedback =
        Feedback(
            id = requireNotNull(this.id) { "Feedback id must not be null after save" },
            category = this.category,
            content = this.content,
            email = this.email,
            ipHash = this.ipHash,
            checked = this.checked,
        )
}
