package com.imstargg.infrastructure.db.core.operation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface FeedbackJpaRepository : JpaRepository<FeedbackEntity, Long> {
    fun findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        from: Instant,
        to: Instant,
    ): List<FeedbackEntity>
}
