package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.Battle
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntityClub
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
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
        battles: List<Battle>,
    ): List<Battle> {
        val player =
            playerRepository.findWithLockByBrawlStarsTag(playerResponse.tag)
                ?: createPlayer(playerResponse)

        playerUpdateApplier.apply(player, playerResponse)
        return battleUpdateApplier.apply(player, battles)
    }

    @Transactional
    fun updatePlayer(response: PlayerResponse) {
        val player =
            playerRepository.findWithLockByBrawlStarsTag(response.tag)
                ?: createPlayer(response)

        playerUpdateApplier.apply(player = player, response = response)
    }

    private fun createPlayer(response: PlayerResponse): PlayerEntity =
        PlayerEntity(
            brawlStarsTag = response.tag,
            name = response.name,
            nameColor = response.nameColor,
            iconBrawlStarsId = response.icon.id,
            trophies = response.trophies,
            highestTrophies = response.highestTrophies,
            expLevel = response.expLevel,
            expPoints = response.expPoints,
            qualifiedFromChampionshipChallenge = response.isQualifiedFromChampionshipChallenge,
            victories3vs3 = response.victories3vs3,
            victoriesSolo = response.victoriesSolo,
            victoriesDuo = response.victoriesDuo,
            bestRoboRumbleTime = response.bestRoboRumbleTime,
            bestTimeAsBigBrawler = response.bestTimeAsBigBrawler,
            totalPrestigeLevel = response.totalPrestigeLevel,
            club = response.club?.let { PlayerEntityClub(it.tag, it.name) },
        )
}
