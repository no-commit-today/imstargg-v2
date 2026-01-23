package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Entity
@Table(name = "stats_battle")
class StatsBattleEntity(
    @Column(name = "created_date", updatable = false, nullable = false)
    val createdDate: LocalDate,
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: OffsetDateTime,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "event_mode", length = 65, updatable = false, nullable = false)
    val eventMode: String,
    @Column(name = "event_map", length = 65, updatable = false, nullable = false)
    val eventMap: String,
    @Column(name = "mode", length = 105, updatable = false)
    val mode: String?,
    @Column(name = "type", length = 105, updatable = false)
    val type: String?,
    @Column(name = "result", length = 25, updatable = false)
    val result: String?,
    @Column(name = "duration", updatable = false)
    val duration: Int?,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @Column(name = "battle_rank", updatable = false)
    val battleRank: Int?,
    @Column(name = "trophy_change", updatable = false)
    val trophyChange: Int?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<BattleEntityTeamPlayer>>,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_battle_id", nullable = false)
    val id: Long? = null

    companion object {
        fun canCreate(battle: BattleEntity): Boolean {
            val event = battle.event ?: return false
            return event.brawlStarsId != null &&
                event.mode != null &&
                event.map != null
        }

        fun from(battle: BattleEntity): StatsBattleEntity {
            require(canCreate(battle)) {
                "생성 불가: 이벤트 필드가 null입니다. battleKey=${battle.battleKey}"
            }

            return StatsBattleEntity(
                createdDate = OffsetDateTime.ofInstant(battle.createdAt!!, ZoneId.of("Asia/Seoul")).toLocalDate(),
                battleKey = battle.battleKey,
                battleAt = battle.battleAt,
                eventBrawlStarsId = battle.event!!.brawlStarsId!!,
                eventMode = battle.event!!.mode!!,
                eventMap = battle.event!!.map!!,
                mode = battle.mode,
                type = battle.type,
                result = battle.result,
                duration = battle.duration,
                starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
                playerBrawlStarsTag = battle.player.brawlStarsTag,
                battleRank = battle.battleRank,
                trophyChange = battle.trophyChange,
                teams = battle.teams,
            )
        }
    }
}
