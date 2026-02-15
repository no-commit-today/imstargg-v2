package com.imstargg.infrastructure.db.core.brawlstars

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "code", length = 65, updatable = false, nullable = false))
    val code: BrawlStarsImageCode,
    @Column(name = "stored_name", updatable = false, nullable = false)
    val storedName: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_image_id")
    val id: Long? = null
}
