package com.imstargg.infrastructure.db.core

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@MappedSuperclass
abstract class BaseEntity {
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant? = null

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

    fun delete() {
        deleted = true
    }

    fun restore() {
        deleted = false
    }

    fun isDeleted() = deleted

    fun isActive() = !deleted
}
