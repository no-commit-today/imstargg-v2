package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "feedback")
class FeedbackEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "category", updatable = false, nullable = false, length = 32)
    val category: FeedbackCategory,
    @Column(name = "content", updatable = false, nullable = false, columnDefinition = "text")
    val content: String,
    @Column(name = "email", updatable = false, length = 254)
    val email: String?,
    @Column(name = "ip_hash", updatable = false, nullable = false, length = 64)
    val ipHash: String,
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
