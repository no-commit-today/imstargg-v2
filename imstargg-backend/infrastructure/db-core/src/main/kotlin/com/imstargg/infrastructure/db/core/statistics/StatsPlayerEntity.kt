package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "stats_player")
class StatsPlayerEntity(
    @Column(name = "brawlstars_tag", updatable = false, nullable = false)
    val brawlStarsTag: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_player_id", nullable = false)
    val id: Long? = null

    companion object {
        fun from(teamPlayer: StatsBattleEntityTeamPlayer) =
            StatsPlayerEntity(
                brawlStarsTag = teamPlayer.brawlStarsTag,
            )
    }
}
