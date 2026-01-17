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
    name = "player_brawler_star_power",
    indexes = [
        Index(
            name = "uk_playerbrawlerstarpower__playerbrawlerid_starpowerbrawlstarsid",
            columnList = "player_brawler_id, star_power_brawlstars_id",
            unique = true,
        ),
    ],
)
class PlayerBrawlerStarPowerEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_brawler_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val playerBrawler: PlayerBrawlerEntity,
    @Column(name = "star_power_brawlstars_id", updatable = false, nullable = false)
    val starPowerBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_star_power_id")
    val id: Long? = null
}
