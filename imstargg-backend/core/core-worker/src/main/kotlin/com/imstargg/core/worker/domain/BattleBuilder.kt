package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse
import com.imstargg.infrastructure.db.core.statistics.battle.BattleKeyEmbedded

class BattleBuilder(
    private val brawlStarsTag: String,
    private val battleResponse: BattleResponse,
) {
    fun build(): Battle {
        val teams = getBattleTeamList()
        val battleKey = createBattleKey(teams)
        return buildBattleResult(battleKey, teams)
    }

    private fun getBattleTeamList(): List<List<BattleResultPlayerResponse>> =
        battleResponse.battle.teams
            ?: battleResponse.battle.players?.map { listOf(it) }
            ?: throw IllegalArgumentException("플레이어 정보가 없습니다. battleResponse=$battleResponse")

    private fun createBattleKey(teams: List<List<BattleResultPlayerResponse>>): String {
        val battleTime = battleResponse.battleTime.toInstant()
        val tags =
            if (battleResponse.battle.rank != null) {
                val playerTeam =
                    teams.firstOrNull { team -> team.any { it.tag == brawlStarsTag } }
                        ?: throw IllegalArgumentException(
                            "수집 대상 플레이어가 팀에 포함되지 않았습니다. tag=$brawlStarsTag",
                        )
                playerTeam.map { it.tag }
            } else {
                teams.flatten().map { it.tag }
            }
        return BattleKeyEmbedded.create(battleTime, tags).value
    }

    private fun buildBattleResult(
        battleKey: String,
        teams: List<List<BattleResultPlayerResponse>>,
    ): Battle {
        val battle = battleResponse.battle
        val event = battleResponse.event
        return Battle(
            battleKey = battleKey,
            battleAt = battleResponse.battleTime.toInstant(),
            event =
                BattleEvent(
                    brawlStarsId = event.id,
                    modeBrawlStarsName = event.mode,
                    modeBrawlStarsId = event.modeId.toLong(),
                    map = event.map,
                ),
            mode = battle.mode,
            type = battle.type,
            result = battle.result,
            duration = battle.duration,
            starPlayerBrawlStarsTag = battle.starPlayer?.tag,
            playerBrawlStarsTag = brawlStarsTag,
            battleRank = battle.rank,
            trophyChange = battle.trophyChange,
            teams = buildTeams(teams),
        )
    }

    private fun buildTeams(teams: List<List<BattleResultPlayerResponse>>): List<List<BattleTeamPlayer>> =
        teams.map { team ->
            team.flatMap { player ->
                getPlayerBrawlers(player).map { brawler ->
                    BattleTeamPlayer(
                        brawlStarsTag = player.tag,
                        name = player.name,
                        brawler =
                            BattleTeamPlayerBrawler(
                                brawlStarsId = brawler.id,
                                name = brawler.name,
                                power = brawler.power,
                                trophies = brawler.trophies,
                                trophyChange = brawler.trophyChange,
                            ),
                    )
                }
            }
        }

    private fun getPlayerBrawlers(player: BattleResultPlayerResponse): List<BattleResultBrawlerResponse> =
        player.brawler?.let { listOf(it) }
            ?: player.brawlers
            ?: throw IllegalArgumentException("브롤러 정보가 없습니다. player=$player")
}
