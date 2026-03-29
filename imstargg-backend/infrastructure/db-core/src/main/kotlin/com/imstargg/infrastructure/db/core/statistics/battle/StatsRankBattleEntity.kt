package com.imstargg.infrastructure.db.core.statistics.battle

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "stats_rank_battle",
    indexes = [
        Index(
            name = "uk_stats_rank_battle__battle_date_battle_key_player",
            columnList = "battle_date, battle_key, player_brawlstars_tag",
            unique = true,
        ),
    ],
)
class StatsRankBattleEntity(
    battleKey: String,
    battleAt: OffsetDateTime,
    eventBrawlStarsId: Long,
    eventModeBrawlStarsId: Long,
    eventMap: String,
    mode: String,
    type: String,
    starPlayerBrawlStarsTag: String?,
    playerBrawlStarsTag: String,
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    @Column(name = "battle_rank", updatable = false, nullable = false)
    val battleRank: Int,
) : BaseStatsBattleEntity(
        battleKey = battleKey,
        battleAt = battleAt,
        eventBrawlStarsId = eventBrawlStarsId,
        eventModeBrawlStarsId = eventModeBrawlStarsId,
        eventMap = eventMap,
        mode = mode,
        type = type,
        starPlayerBrawlStarsTag = starPlayerBrawlStarsTag,
        playerBrawlStarsTag = playerBrawlStarsTag,
        teams = teams,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_rank_battle_id", nullable = false)
    val id: Long? = null
}
