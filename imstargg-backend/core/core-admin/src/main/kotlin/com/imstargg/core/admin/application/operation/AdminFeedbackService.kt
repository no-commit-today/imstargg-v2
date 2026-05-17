package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminFeedbackService(
    private val repository: AdminFeedbackRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        category: FeedbackCategory?,
        checked: Boolean?,
        page: Int,
        size: Int,
    ): Page<FeedbackEntity> = repository.search(category, checked, PageRequest.of(page, size))

    @Transactional
    fun updateCheck(
        id: Long,
        checked: Boolean,
    ) {
        val entity =
            repository.findById(id)
                ?: throw IllegalArgumentException("Feedback not found: id=$id")
        if (checked) entity.check() else entity.uncheck()
    }
}
