package com.imstargg.infrastructure.db.core.brawlstars

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
    name = "brawlstars_image",
    indexes = [
        Index(name = "uk_brawlstars_image__type_brawlstarsid", columnList = "type, brawlstars_id", unique = true),
    ],
)
class BrawlStarsImageEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 45, updatable = false, nullable = false)
    val type: BrawlStarsImageType,
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "stored_name", updatable = false, nullable = false)
    val storedName: String,
    @Column(name = "url", nullable = false)
    val url: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_image_id")
    val id: Long? = null

    companion object {
        fun storedName(
            type: BrawlStarsImageType,
            brawlStarsId: Long,
        ): String =
            when (type) {
                BrawlStarsImageType.BRAWLER_PORTRAIT -> "brawler/portrait/$brawlStarsId"
                BrawlStarsImageType.STARPOWER -> "starpower/$brawlStarsId"
                BrawlStarsImageType.GADGET -> "gadget/$brawlStarsId"
                BrawlStarsImageType.GEAR -> "gear/$brawlStarsId"
                BrawlStarsImageType.HYPERCHARGE -> "hypercharge/$brawlStarsId"
                BrawlStarsImageType.BATTLE_EVENT_MODE_ICON -> "event/mode/icon/$brawlStarsId"
                BrawlStarsImageType.BUFFY_GADGET -> "brawler/buffy_gadget/$brawlStarsId"
                BrawlStarsImageType.BUFFY_STAR_POWER -> "brawler/buffy_starpower/$brawlStarsId"
                BrawlStarsImageType.BUFFY_HYPER_CHARGE -> "brawler/buffy_hypercharge/$brawlStarsId"
            }
    }
}
