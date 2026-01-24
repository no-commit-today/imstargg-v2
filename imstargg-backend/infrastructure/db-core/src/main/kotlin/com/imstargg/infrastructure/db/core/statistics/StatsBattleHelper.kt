package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankTier
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import java.time.Clock
import java.time.OffsetDateTime

object StatsBattleHelper {
    const val BATTLE_MAX_AGE_DAYS = 30L

    private const val MIN_TROPHY = 500
    private val MIN_SOLO_RANK_TIER = SoloRankTier.DIAMOND_1

    fun canAggregate(clock: Clock, battle: BattleEntity) : Boolean {
        val now = OffsetDateTime.now(clock)
        return battle.battleAt.isAfter(now.minusDays(BATTLE_MAX_AGE_DAYS))
    }

    fun canCreateRankBattle(battle: BattleEntity): Boolean {
        if (!canCreate(battle)) {
            return false
        }
        if (battle.battleRank == null) {
            return false
        }
        return true
    }

    fun canCreateResultBattle(battle: BattleEntity): Boolean {
        if (!canCreate(battle)) {
            return false
        }
        if (battle.result == null) {
            return false
        }

        return true
    }

    /**
     * 공통 검증: 모든 Stats Battle 엔티티에 적용
     * - event 필드 (brawlStarsId, mode, map) not null
     * - mode, type not null
     * - type != FRIENDLY
     * - 모든 팀 플레이어 브롤러 트로피 >= 500
     * - SOLO_RANKED인 경우 트로피(SoloRankTier 값) > DIAMOND_1 (value 10)
     */
    private fun canCreate(battle: BattleEntity): Boolean {
        val event = battle.event ?: return false

        if (event.brawlStarsId == null || event.mode == null || event.map == null) {
            return false
        }
        if (battle.mode == null || battle.type == null) {
            return false
        }

        val battleType = EnumCodeType.findByCode<BattleType>(battle.type!!) ?: return false
        if (battleType == BattleType.FRIENDLY) {
            return false
        }

        val minTrophies =
            battle.teams
                .flatten()
                .minOfOrNull { it.brawler.trophies }
                ?: return false

        if (battleType == BattleType.SOLO_RANKED && minTrophies < MIN_SOLO_RANK_TIER.value) {
            return false
        }

        if (minTrophies < MIN_TROPHY) {
            return false
        }

        return true
    }

    /**
     * teams 변환: BattleEntityTeamPlayer → StatsBattleEntityTeamPlayer
     */
    fun convertTeams(teams: List<List<BattleEntityTeamPlayer>>): List<List<StatsBattleEntityTeamPlayer>> =
        teams.map { team ->
            team.map { player -> StatsBattleEntityTeamPlayer.from(player) }
        }
}
