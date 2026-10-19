package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.core.enums.GearRarity
import com.imstargg.infrastructure.db.core.BaseSoftDeleteEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "gear",
    indexes = [
        Index(name = "uk_gear__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class GearEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val rarity: GearRarity,
) : BaseSoftDeleteEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gear_id")
    val id: Long? = null
}
