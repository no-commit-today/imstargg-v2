package com.imstargg.core.worker.application.stats

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsEventModeType
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
import java.time.Duration

@Component
class StatsBattleAggregateApplier(
    private val clock: Clock,
    private val statsRankBattleRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleRepository: StatsResultBattleJpaRepository,
    private val statsEventModeSnapshot: StatsEventModeSnapshot,
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
        val now = clock.instant()
        return battle.battleAt.isAfter(now.minus(Duration.ofDays(BATTLE_MAX_AGE_DAYS)))
    }

    /**
     * 통계로 수집할 수 있는 전투면 그 모드의 유형을 돌려주고, 아니면 null 을 돌려준다.
     *
     * 모드는 `battle.mode` 문자열이 아니라 `modeBrawlStarsId` 로만 판정한다.
     * 플레이어 한 명이 브롤러 여러 개를 낸 전투(듀얼 펼침)는 유형과 관계없이 거른다.
     * 펼침은 전적 표시용이고, 통계에서는 한 명의 브롤러들이 팀원으로 오해된다.
     *
     * battlelog 는 하키(45)를 `brawlBall`, 아레나(48)를 `siege` 로 보내는 등 문자열을 믿을 수 없다.
     */
    private fun findStatsEventModeType(battle: Battle): StatsEventModeType? {
        if (battle.brawlersExpanded) return null
        val event = battle.event ?: return null
        if (event.brawlStarsId == null || event.modeBrawlStarsId == null || event.map == null) return null
        if (battle.mode == null || battle.type == null) return null

        val battleType = EnumCodeType.findByCode<BattleType>(battle.type) ?: return null
        if (battleType == BattleType.FRIENDLY) return null

        val allPlayers = battle.teams.flatten()
        val brawlerIds = allPlayers.map { it.brawler.brawlStarsId }
        if (brawlerIds.size != brawlerIds.distinct().size) return null

        val minTrophies = allPlayers.minOfOrNull { it.brawler.trophies } ?: return null
        if (battleType == BattleType.SOLO_RANKED) {
            if (minTrophies < StatsTierRange.MIN_SOLO_RANK_TIER.value) return null
        } else {
            if (minTrophies < StatsTierRange.MIN_TROPHY) return null
        }

        val type = statsEventModeSnapshot.find(event.modeBrawlStarsId) ?: return null
        if (!type.matches(battle.teams.map { it.size })) return null
        return type
    }

    private fun canCreateRankBattle(battle: Battle): Boolean {
        if (battle.battleRank == null) return false
        return findStatsEventModeType(battle)?.resultType == BattleEventModeResultType.RANK
    }

    private fun canCreateResultBattle(battle: Battle): Boolean {
        if (battle.result == null) return false
        return findStatsEventModeType(battle)?.resultType == BattleEventModeResultType.RESULT
    }

    private fun toStatsRankBattle(battle: Battle) =
        StatsRankBattleEntity(
            battleKey = battle.battleKey,
            battleAt = battle.battleAt,
            eventBrawlStarsId = battle.event!!.brawlStarsId!!,
            eventModeBrawlStarsId = battle.event.modeBrawlStarsId!!,
            eventMap = battle.event.map!!,
            mode = battle.mode!!,
            type = battle.type!!,
            playerBrawlStarsTag = battle.playerBrawlStarsTag,
            battleRank = battle.battleRank!!,
            teams = convertTeams(battle),
        )

    private fun toStatsResultBattle(battle: Battle) =
        StatsResultBattleEntity(
            battleKey = battle.battleKey,
            battleAt = battle.battleAt,
            eventBrawlStarsId = battle.event!!.brawlStarsId!!,
            eventModeBrawlStarsId = battle.event.modeBrawlStarsId!!,
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
                            // 통계 JSON 은 jackson-module-kotlin 생성자 주입으로 읽혀 non-null 파라미터에 null 이 오면
                            // 구버전 core-batch Reader 가 실패한다 — 빈 문자열이면 배포 순서·롤백 제약이 없다
                            name = player.brawler.name ?: "",
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
