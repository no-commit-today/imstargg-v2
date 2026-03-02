package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_event_solo_rank_event",
    indexes = [
        Index(
            name = "uk_battle_event_solo_rank_event__1",
            columnList = "solo_rank_season_key, event_brawlstars_id",
            unique = true,
        ),
    ],
)
class BattleEventSoloRankEventEntity(
    @Column(name = "solo_rank_season_key", length = 45, updatable = false, nullable = false)
    val soloRankSeasonKey: String,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_solo_rank_event_id")
    val id: Long? = null
}
