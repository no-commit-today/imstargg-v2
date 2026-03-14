package com.imstargg.core.worker.application

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.worker.domain.BattleResult
import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedRankBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedRankBattleKeyJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedResultBattleKeyEntity
import com.imstargg.infrastructure.db.core.statistics.battle.AggregatedResultBattleKeyJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StatsBattleAggregateApplier(
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
    private val aggregatedRankBattleKeyRepository: AggregatedRankBattleKeyJpaRepository,
    private val aggregatedResultBattleKeyRepository: AggregatedResultBattleKeyJpaRepository,
) {
    @Transactional
    fun apply(battles: List<BattleResult>) {
        val statsRankBattles =
            battles
                .filter { canCreateRankBattle(it) }
                .map { toStatsRankBattle(it) }

        val statsResultBattles =
            battles
                .filter { canCreateResultBattle(it) }
                .map { toStatsResultBattle(it) }

        val aggregatedRankKeys =
            aggregatedRankBattleKeyRepository
                .findAllByStatsRankBattles(statsRankBattles)
        val aggregatedResultKeys =
            aggregatedResultBattleKeyRepository
                .findAllByStatsResultBattles(statsResultBattles)

        val newRankBattles =
            statsRankBattles.filter { battle ->
                aggregatedRankKeys.none { it.matches(battle) }
            }
        val newResultBattles =
            statsResultBattles.filter { battle ->
                aggregatedResultKeys.none { it.matches(battle) }
            }

        statsRankBattleRepository.saveAll(newRankBattles)
        statsResultBattleRepository.saveAll(newResultBattles)

        aggregatedRankBattleKeyRepository.saveAll(
            newRankBattles.map { AggregatedRankBattleKeyEntity.from(it) },
        )
        aggregatedResultBattleKeyRepository.saveAll(
            newResultBattles.map { AggregatedResultBattleKeyEntity.from(it) },
        )
    }

    private fun canCreateStatsBattle(battle: BattleResult): Boolean {
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

    private fun canCreateRankBattle(battle: BattleResult): Boolean {
        if (battle.battleRank == null) return false
        return canCreateStatsBattle(battle)
    }

    private fun canCreateResultBattle(battle: BattleResult): Boolean {
        if (battle.result == null) return false
        return canCreateStatsBattle(battle)
    }

    private fun toStatsRankBattle(battle: BattleResult) =
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

    private fun toStatsResultBattle(battle: BattleResult) =
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

    private fun convertTeams(battle: BattleResult): List<List<StatsBattleEntityTeamPlayer>> =
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
}
