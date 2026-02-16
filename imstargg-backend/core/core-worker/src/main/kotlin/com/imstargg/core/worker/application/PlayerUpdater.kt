package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.worker.domain.BattleEntityBuilder
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntityClub
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.lock.LockName
import com.imstargg.infrastructure.db.core.lock.LockTemplate
import org.springframework.stereotype.Component

@Component
class PlayerUpdater(
    private val lockTemplate: LockTemplate,
    private val playerRepository: PlayerJpaRepository,
    private val playerUpdateApplier: PlayerUpdateApplier,
    private val battleUpdateApplier: BattleUpdateApplier,
    private val statsBattleAggregator: StatsBattleAggregator,
) {
    companion object {
        private const val LOCK_TIMEOUT_SECONDS = 3
    }

    fun update(
        playerResponse: PlayerResponse,
        battleResponses: List<BattleResponse>,
    ) {
        val savedBattles =
            lockTemplate.execute(
                LockName.createPlayerLockName(playerResponse.tag),
                LOCK_TIMEOUT_SECONDS,
            ) {
                val player =
                    playerRepository.findByBrawlStarsTag(playerResponse.tag)
                        ?: createPlayer(playerResponse)

                playerUpdateApplier.apply(player, playerResponse)

                val buildResults = battleResponses.map { BattleEntityBuilder(player, it).build() }
                battleUpdateApplier.apply(player, buildResults)
            }

        statsBattleAggregator.asyncAggregate(savedBattles)
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
            club = response.club?.let { PlayerEntityClub(it.tag, it.name) },
        )
}
