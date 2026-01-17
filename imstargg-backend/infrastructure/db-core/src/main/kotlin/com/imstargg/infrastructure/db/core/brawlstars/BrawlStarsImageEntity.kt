package com.imstargg.infrastructure.db.core.brawlstars

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "brawlstars_image",
    indexes = [
        Index(name = "uk_brawlstars_image__code", columnList = "code", unique = true),
    ],
)
class BrawlStarsImageEntity(
    @Column(name = "code", length = 65, updatable = false, nullable = false)
    val code: String,
    @Column(name = "stored_name", updatable = false, nullable = false)
    val storedName: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_image_id")
    val id: Long? = null
}
