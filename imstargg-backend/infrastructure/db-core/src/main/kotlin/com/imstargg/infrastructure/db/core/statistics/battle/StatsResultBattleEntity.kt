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
    name = "stats_result_battle",
    indexes = [
        Index(
            name = "uk_stats_result_battle__battle_date_battle_key",
            columnList = "battle_date, battle_key",
            unique = true,
        ),
        Index(
            name = "ix_stats_result_battle__battle_date_event",
            columnList = "battle_date, event_brawlstars_id",
        ),
    ],
)
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
