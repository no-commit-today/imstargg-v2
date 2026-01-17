package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.BattleEntityBuilder
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlayerUpdater(
    private val playerRepository: PlayerJpaRepository,
    private val playerUpdateApplier: PlayerUpdateApplier,
    private val battleUpdateApplier: BattleUpdateApplier,
) {
    @Transactional
    fun update(
        playerResponse: PlayerResponse,
        battleResponses: List<BattleResponse>,
    ) {
        val player =
            playerRepository.findWithLockByBrawlStarsTag(playerResponse.tag)
                ?: throw IllegalArgumentException("Player not found: ${playerResponse.tag}")

        playerUpdateApplier.apply(player, playerResponse)

        val battleEntities = battleResponses.map { BattleEntityBuilder(player, it).build() }
        battleUpdateApplier.apply(player, battleEntities)
    }
}
