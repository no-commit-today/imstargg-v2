package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class FeedbackRepository(
    private val jpaRepository: FeedbackJpaRepository,
) {
    @Transactional
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
            id = requireNotNull(id) { "Feedback id must not be null after save" },
            category = category,
            content = content,
            email = email,
            ipHash = ipHash,
            checked = checked,
        )
}
