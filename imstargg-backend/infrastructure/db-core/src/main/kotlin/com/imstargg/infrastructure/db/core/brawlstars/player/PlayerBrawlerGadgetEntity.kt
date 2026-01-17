package com.imstargg.infrastructure.db.core.brawlstars.player

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "player_brawler_gadget",
    indexes = [
        Index(
            name = "uk_playerbrawlergadget__playerbrawlerid_gadgetbrawlstarsid",
            columnList = "player_brawler_id, gadget_brawlstars_id",
            unique = true,
        ),
    ],
)
class PlayerBrawlerGadgetEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_brawler_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val playerBrawler: PlayerBrawlerEntity,
    @Column(name = "gadget_brawlstars_id", updatable = false, nullable = false)
    val gadgetBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_gadget_id")
    val id: Long? = null
}
