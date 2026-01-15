package com.imstargg.storage.db.core.brawlstars

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "brawlstars_image",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_brawlstars_image__code", columnNames = ["code"]),
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
