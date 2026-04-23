package com.imstargg.core.worker.application

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.worker.domain.Battle
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime

@Component
class StatsBattleAggregateApplier(
    private val clock: Clock,
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
) {
    @Transactional
    fun apply(battles: List<Battle>) {
        val aggregatableBattles = battles.filter { canAggregate(it) }

        val statsRankBattles =
            aggregatableBattles
                .filter { canCreateRankBattle(it) }
                .map { toStatsRankBattle(it) }
                .distinctBy { it.battleKey }

        val statsResultBattles =
            aggregatableBattles
                .filter { canCreateResultBattle(it) }
                .map { toStatsResultBattle(it) }
                .distinctBy { it.battleKey }

        val existingResultBattleKeys =
            statsResultBattleRepository.findAllBattleKeysByStatsResultBattles(statsResultBattles)
        val existingRankBattleKeys =
            statsRankBattleRepository.findAllBattleKeysByStatsRankBattles(statsRankBattles)

        val newRankBattles = statsRankBattles.filter { it.battleKey !in existingRankBattleKeys }
        val newResultBattles = statsResultBattles.filter { it.battleKey !in existingResultBattleKeys }

        statsRankBattleRepository.saveAll(newRankBattles)
        statsResultBattleRepository.saveAll(newResultBattles)
    }

    private fun canAggregate(battle: Battle): Boolean {
        val now = OffsetDateTime.now(clock)
        return battle.battleAt.isAfter(now.minusDays(BATTLE_MAX_AGE_DAYS))
    }

    private fun canCreateStatsBattle(battle: Battle): Boolean {
        val event = battle.event ?: return false
        if (event.brawlStarsId == null || event.modeBrawlStarsId == null || event.map == null) return false
        if (battle.mode == null || battle.type == null) return false

        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return false
        if (battleType == BattleType.FRIENDLY) return false

        val allPlayers = battle.teams.flatten()
        val brawlerIds = allPlayers.map { it.brawler.brawlStarsId }
        if (brawlerIds.size != brawlerIds.distinct().size) return false

        val minTrophies = allPlayers.minOfOrNull { it.brawler.trophies } ?: return false
        if (battleType == BattleType.SOLO_RANKED) {
            if (minTrophies < StatsTierRange.MIN_SOLO_RANK_TIER.value) return false
        } else {
            if (minTrophies < StatsTierRange.MIN_TROPHY) return false
        }
        return true
    }

    private fun canCreateRankBattle(battle: Battle): Boolean {
        if (battle.battleRank == null) return false
        return canCreateStatsBattle(battle)
    }

    private fun canCreateResultBattle(battle: Battle): Boolean {
        if (battle.result == null) return false
        return canCreateStatsBattle(battle)
    }

    private fun toStatsRankBattle(battle: Battle) =
        StatsRankBattleEntity(
            battleKey = battle.battleKey,
            battleAt = battle.battleAt,
            eventBrawlStarsId = battle.event!!.brawlStarsId!!,
            eventModeBrawlStarsId = battle.event.modeBrawlStarsId!!.toLong(),
            eventMap = battle.event.map!!,
            mode = battle.mode!!,
            type = battle.type!!,
            starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
            playerBrawlStarsTag = battle.playerBrawlStarsTag,
            battleRank = battle.battleRank!!,
            teams = convertTeams(battle),
        )

    private fun toStatsResultBattle(battle: Battle) =
        StatsResultBattleEntity(
            battleKey = battle.battleKey,
            battleAt = battle.battleAt,
            eventBrawlStarsId = battle.event!!.brawlStarsId!!,
            eventModeBrawlStarsId = battle.event.modeBrawlStarsId!!.toLong(),
            eventMap = battle.event.map!!,
            mode = battle.mode!!,
            type = battle.type!!,
            starPlayerBrawlStarsTag = battle.starPlayerBrawlStarsTag,
            playerBrawlStarsTag = battle.playerBrawlStarsTag,
            result = battle.result!!,
            teams = convertTeams(battle),
        )

    private fun convertTeams(battle: Battle): List<List<StatsBattleEntityTeamPlayer>> =
        battle.teams.map { team ->
            team.map { player ->
                StatsBattleEntityTeamPlayer(
                    brawlStarsTag = player.brawlStarsTag,
                    name = player.name,
                    brawler =
                        StatsBattleEntityTeamPlayerBrawler(
                            brawlStarsId = player.brawler.brawlStarsId,
                            name = player.brawler.name,
                            power = player.brawler.power,
                            trophies = player.brawler.trophies,
                        ),
                )
            }
        }

    companion object {
        private const val BATTLE_MAX_AGE_DAYS = 7L
    }
}
