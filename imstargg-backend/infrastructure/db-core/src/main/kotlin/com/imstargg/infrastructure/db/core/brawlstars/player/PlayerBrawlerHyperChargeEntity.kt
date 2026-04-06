package com.imstargg.infrastructure.db.core.brawlstars.player

import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
    name = "player_brawler_hyper_charge",
    indexes = [
        Index(
            name = "uk_playerbrawlerhypercharge__playerbrawlerid_hyperchargebrawlstarsid",
            columnList = "player_brawler_id, hyper_charge_brawlstars_id",
            unique = true,
        ),
    ],
)
class PlayerBrawlerHyperChargeEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_brawler_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val playerBrawler: PlayerBrawlerEntity,
    @Column(name = "hyper_charge_brawlstars_id", updatable = false, nullable = false)
    val hyperChargeBrawlStarsId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_hyper_charge_id")
    val id: Long? = null
}
