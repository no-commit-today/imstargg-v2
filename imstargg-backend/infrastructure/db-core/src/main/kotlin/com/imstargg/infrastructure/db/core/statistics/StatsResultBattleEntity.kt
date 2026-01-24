package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
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

@Entity
@Table(name = "stats_result_battle")
class StatsResultBattleEntity(
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
    @Column(name = "mode", length = 65, updatable = false, nullable = false)
    val mode: String,
    @Column(name = "type", length = 65, updatable = false, nullable = false)
    val type: String,
    @Column(name = "result", length = 25, updatable = false, nullable = false)
    val result: String,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_result_battle_id", nullable = false)
    val id: Long? = null

    companion object {
        fun from(battle: BattleEntity): StatsResultBattleEntity {
            require(StatsBattleHelper.canCreateResultBattle(battle)) {
                "StatsResultBattleEntity 생성 불가: result가 null이거나 DUELS 모드이거나 공통 검증 실패. battleKey=${battle.battleKey}"
            }

            return StatsResultBattleEntity(
                createdDate = battle.createdAt!!.toKoreaLocalDate(),
                battleKey = battle.battleKey,
                battleAt = battle.battleAt,
                eventBrawlStarsId = battle.event!!.brawlStarsId!!,
                eventMode = battle.event!!.mode!!,
                eventMap = battle.event!!.map!!,
                mode = battle.mode!!,
                type = battle.type!!,
                result = battle.result!!,
                starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
                playerBrawlStarsTag = battle.player.brawlStarsTag,
                teams = StatsBattleHelper.convertTeams(battle.teams),
            )
        }
    }
}
