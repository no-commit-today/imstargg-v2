package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.QFeedbackEntity.Companion.feedbackEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class AdminFeedbackRepository(
    entityManager: EntityManager,
) {
    private val queryFactory = JPAQueryFactory(entityManager)

    fun search(
        category: FeedbackCategory?,
        checked: Boolean?,
        pageable: Pageable,
    ): Page<FeedbackEntity> {
        val predicates =
            listOfNotNull(
                category?.let { feedbackEntity.category.eq(it) },
                checked?.let { feedbackEntity.checked.eq(it) },
            ).toTypedArray()

        val content =
            queryFactory
                .selectFrom(feedbackEntity)
                .where(*predicates)
                .orderBy(feedbackEntity.createdAt.desc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val total =
            queryFactory
                .select(feedbackEntity.count())
                .from(feedbackEntity)
                .where(*predicates)
                .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    fun findById(id: Long): FeedbackEntity? =
        queryFactory
            .selectFrom(feedbackEntity)
            .where(feedbackEntity.id.eq(id))
            .fetchOne()
}
