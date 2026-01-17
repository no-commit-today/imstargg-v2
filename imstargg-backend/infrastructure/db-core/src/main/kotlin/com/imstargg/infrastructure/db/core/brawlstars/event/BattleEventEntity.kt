package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_event",
    indexes = [
        Index(name = "uk_battle_event__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "mode", length = 45, updatable = false, nullable = false)
    val mode: String,
    @Column(name = "map_brawlstars_name", length = 105, updatable = false)
    val mapBrawlStarsName: String?,
    @Column(name = "solo_ranked", nullable = false, updatable = false)
    val soloRanked: Boolean,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_id")
    val id: Long? = null
}
