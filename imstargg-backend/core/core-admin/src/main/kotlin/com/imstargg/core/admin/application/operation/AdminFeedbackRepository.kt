package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
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
    private val jpaRepository: FeedbackJpaRepository,
) {
    private val queryFactory = JPAQueryFactory(entityManager)

    fun search(
        rating: FeedbackRating?,
        checked: Boolean?,
        pageable: Pageable,
    ): Page<FeedbackEntity> {
        val predicates =
            listOfNotNull(
                rating?.let { feedbackEntity.rating.eq(it) },
                checked?.let { feedbackEntity.checked.eq(it) },
            ).toTypedArray()

        val content =
            queryFactory
                .selectFrom(feedbackEntity)
                .where(*predicates)
                .orderBy(feedbackEntity.submittedAt.desc())
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

    fun findById(id: Long): FeedbackEntity? = jpaRepository.findById(id).orElse(null)
}
