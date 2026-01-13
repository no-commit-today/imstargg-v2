package com.imstargg.storage.db.core.brawlstars.player

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
    name = "player_brawler_gadget",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid",
            columnNames = ["player_brawler_id", "gadget_brawlstars_id"],
        ),
    ],
)
class PlayerBrawlerGadgetEntity(
    @Column(name = "player_brawler_id", updatable = false, nullable = false)
    val playerBrawlerId: Long,
    @Column(name = "gadget_brawlstars_id", updatable = false, nullable = false)
    val gadgetBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_gadget_id")
    val id: Long? = null
}
