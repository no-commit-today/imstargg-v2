package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.battle.BattlePlayer
import com.imstargg.core.enums.SoloRankTier

data class BattlePlayerResponse(
    val tag: String,
    val name: String,
    val soloRankTier: SoloRankTier?,
    val brawler: BattlePlayerBrawlerResponse,
) {
    companion object {
        fun of(player: BattlePlayer) =
            BattlePlayerResponse(
                tag = player.tag.value,
                name = player.name,
                soloRankTier = player.soloRankTier,
                brawler = BattlePlayerBrawlerResponse.of(player.brawler),
            )
    }
}
