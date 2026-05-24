package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "feedback")
class FeedbackEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", updatable = false, nullable = false, length = 32)
    val rating: FeedbackRating,
    @Column(name = "content", updatable = false, nullable = false, columnDefinition = "text")
    val content: String,
    @Column(name = "submitted_at", updatable = false, nullable = false)
    val submittedAt: Instant,
    checked: Boolean = false,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

    @Column(name = "checked", nullable = false)
    var checked: Boolean = checked
        protected set

    fun check() {
        this.checked = true
    }

    fun uncheck() {
        this.checked = false
    }
}
