package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse

class BattleBuilder(
    private val brawlStarsTag: String,
    private val battleResponse: BattleResponse,
) {
    fun build(): Battle {
        val teams = getBattleTeamList()
        val battleKey = createBattleKey(teams)
        return buildBattle(battleKey, teams)
    }

    private fun getBattleTeamList(): List<List<BattleResultPlayerResponse>> =
        battleResponse.battle.teams
            ?: battleResponse.battle.players?.map { listOf(it) }
            ?: throw IllegalArgumentException("플레이어 정보가 없습니다. battleResponse=$battleResponse")

    private fun createBattleKey(teams: List<List<BattleResultPlayerResponse>>): String {
        val builder = BattleKeyBuilder(battleResponse.battleTime)
        teams.flatten().forEach { builder.addPlayerTag(it.tag) }
        return builder.build()
    }

    private fun buildBattle(
        battleKey: String,
        teams: List<List<BattleResultPlayerResponse>>,
    ): Battle {
        val battle = battleResponse.battle
        val event = battleResponse.event
        return Battle(
            battleKey = battleKey,
            battleAt = battleResponse.battleTime,
            event =
                BattleEvent(
                    brawlStarsId = event.id,
                    modeBrawlStarsName = event.mode,
                    modeBrawlStarsId = event.modeId,
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
