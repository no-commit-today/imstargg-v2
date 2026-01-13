package com.imstargg.storage.db.core

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null

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
