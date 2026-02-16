package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "stats_result_battle")
class StatsResultBattleEntity(
    createdDate: LocalDate,
    battleKey: String,
    battleAt: OffsetDateTime,
    eventBrawlStarsId: Long,
    eventMode: String,
    eventMap: String,
    mode: String,
    type: String,
    starPlayerBrawlStarsTag: String?,
    playerBrawlStarsTag: String,
    teams: List<List<StatsBattleEntityTeamPlayer>>,
    @Column(name = "result", length = 25, updatable = false, nullable = false)
    val result: String,
) : BaseStatsBattleEntity(
        createdDate = createdDate,
        battleKey = battleKey,
        battleAt = battleAt,
        eventBrawlStarsId = eventBrawlStarsId,
        eventMode = eventMode,
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

    companion object {
        fun canCreate(
            battle: BattleEntity,
            teamPlayers: List<BattleTeamPlayerEntity>,
        ): Boolean {
            if (!BaseStatsBattleEntity.canCreate(battle, teamPlayers)) return false
            return battle.result != null
        }

        fun from(
            battle: BattleEntity,
            teamPlayers: List<BattleTeamPlayerEntity>,
        ): StatsResultBattleEntity {
            require(canCreate(battle, teamPlayers)) {
                "StatsResultBattleEntity 생성 불가: result가 null이거나 공통 검증 실패. battleKey=${battle.battleKey}"
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
                teams = convertTeams(teamPlayers),
            )
        }
    }
}
