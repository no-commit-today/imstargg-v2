package com.imstargg.infrastructure.db.core.statistics.battle

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.OffsetDateTime

@MappedSuperclass
abstract class BaseStatsBattleEntity(
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: OffsetDateTime,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "event_mode_brawlstars_id", updatable = false, nullable = false)
    val eventModeBrawlStarsId: Long,
    @Column(name = "event_map", length = 65, updatable = false, nullable = false)
    val eventMap: String,
    @Column(name = "mode", length = 65, updatable = false, nullable = false)
    val mode: String,
    @Column(name = "type", length = 65, updatable = false, nullable = false)
    val type: String,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "jsonb", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseTimeEntity() {
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate = battleAt.toKoreaLocalDate()

    /**
     * 일반적으로 player team 은 1개일 것.
     */
    fun playerTeams(): List<List<StatsBattleEntityTeamPlayer>> =
        teams.filter { team -> team.any { it.brawlStarsTag == playerBrawlStarsTag } }

    fun enemyTeams(): List<List<StatsBattleEntityTeamPlayer>> =
        teams.filter { team -> team.none { it.brawlStarsTag == playerBrawlStarsTag } }
}
