package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "star_power",
    indexes = [
        Index(name = "uk_starpower__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class StarPowerEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "star_power_id")
    val id: Long? = null

    fun nameMessageCode(): MessageCode = MessageCode.of("starpower", brawlStarsId, "name")

    fun imageCode(): BrawlStarsImageCode = BrawlStarsImageCode.of("starpower", brawlStarsId)
}
