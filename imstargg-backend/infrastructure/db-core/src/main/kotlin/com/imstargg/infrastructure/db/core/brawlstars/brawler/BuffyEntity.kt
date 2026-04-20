package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
    name = "buffy",
    indexes = [
        Index(name = "uk_buffy__brawlerbrawlstarsid_type", columnList = "brawler_brawlstars_id, type", unique = true),
    ],
)
class BuffyEntity(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val type: BuffyType,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buffy_id")
    val id: Long? = null
}
