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
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_image_id")
    val id: Long? = null

    companion object {
        fun storedName(
            type: BrawlStarsImageType,
            brawlStarsId: Long,
            ext: String,
        ): String =
            when (type) {
                BrawlStarsImageType.BRAWLER_PORTRAIT -> "brawler/portrait/$brawlStarsId.$ext"
                BrawlStarsImageType.STARPOWER -> "starpower/$brawlStarsId.$ext"
                BrawlStarsImageType.GADGET -> "gadget/$brawlStarsId.$ext"
                BrawlStarsImageType.GEAR -> "gear/$brawlStarsId.$ext"
                BrawlStarsImageType.HYPERCHARGE -> "hypercharge/$brawlStarsId.$ext"
                BrawlStarsImageType.BATTLE_EVENT_MODE_ICON -> "event/mode/icon/$brawlStarsId.$ext"
                BrawlStarsImageType.BUFFY_GADGET -> "brawler/buffy_gadget/$brawlStarsId.$ext"
                BrawlStarsImageType.BUFFY_STAR_POWER -> "brawler/buffy_starpower/$brawlStarsId.$ext"
                BrawlStarsImageType.BUFFY_HYPER_CHARGE -> "brawler/buffy_hypercharge/$brawlStarsId.$ext"
            }
    }
}
