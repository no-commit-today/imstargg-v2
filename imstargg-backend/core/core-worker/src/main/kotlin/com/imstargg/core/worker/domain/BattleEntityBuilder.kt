package com.imstargg.core.worker.domain

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BattleResultBrawlerResponse
import com.imstargg.client.brawlstars.response.BattleResultPlayerResponse
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayerBrawler
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity

class BattleEntityBuilder(
    private val playerEntity: PlayerEntity,
    private val battleResponse: BattleResponse,
) {
    fun build(): BattleEntity {
        val teams = getBattleTeamList()
        val battleKey = createBattleKey(teams)
        val battleTeamPlayers = buildBattleTeamPlayers(teams)
        return buildBattleEntity(battleKey, battleTeamPlayers)
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

    private fun buildBattleEntity(
        battleKey: String,
        teams: List<List<BattleEntityTeamPlayer>>,
    ): BattleEntity {
        val battle = battleResponse.battle
        val event = battleResponse.event
        return BattleEntity(
            battleKey = battleKey,
            battleTime = battleResponse.battleTime,
            event =
                BattleEntityEvent(
                    brawlStarsId = event.id,
                    mode = event.mode,
                    map = event.map,
                ),
            mode = battle.mode,
            type = battle.type,
            result = battle.result,
            duration = battle.duration,
            starPlayerBrawlStarsTag = battle.starPlayer?.tag,
            player =
                BattleEntityPlayer(
                    playerId = playerEntity.id!!,
                    rank = battle.rank,
                    trophyChange = battle.trophyChange,
                ),
            teams = teams,
        )
    }

    private fun buildBattleTeamPlayers(teams: List<List<BattleResultPlayerResponse>>): List<List<BattleEntityTeamPlayer>> =
        teams.map { team ->
            team.flatMap { player ->
                getPlayerBrawlers(player).map { brawler ->
                    BattleEntityTeamPlayer(
                        brawlStarsTag = player.tag,
                        name = player.name,
                        brawler =
                            BattleEntityTeamPlayerBrawler(
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
