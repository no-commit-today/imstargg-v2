package com.imstargg.infrastructure.db.core.brawlstars.event

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
    name = "battle_event",
    indexes = [
        Index(name = "uk_battle_event__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "battle_event_mode_id",
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val mode: BattleEventModeEntity?,
    @Column(name = "map_name", length = 105, updatable = false)
    val mapName: String?,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_id")
    val id: Long? = null
}
