package com.imstargg.infrastructure.db.core.statistics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "stats_result_battle")
class StatsResultBattleEntity(
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
    @Column(name = "result", length = 25, updatable = false, nullable = false)
    val result: String,
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
    @Column(name = "stats_result_battle_id", nullable = false)
    val id: Long? = null
}
