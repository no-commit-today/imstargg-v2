package com.imstargg.infrastructure.db.core.statistics.battle

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
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
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseTimeEntity() {
    @CreationTimestamp
    @Column(name = "created_date", updatable = false, nullable = false)
    val createdDate: LocalDate? = null
}
