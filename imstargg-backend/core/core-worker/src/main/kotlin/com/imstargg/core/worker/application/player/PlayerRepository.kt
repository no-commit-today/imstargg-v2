package com.imstargg.core.worker.application.player

import com.imstargg.core.worker.domain.Player
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Component

@Component
class PlayerRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val playerBrawlerJpaRepository: PlayerBrawlerJpaRepository,
    private val playerBrawlerGearJpaRepository: PlayerBrawlerGearJpaRepository,
    private val playerBrawlerStarPowerJpaRepository: PlayerBrawlerStarPowerJpaRepository,
    private val playerBrawlerGadgetJpaRepository: PlayerBrawlerGadgetJpaRepository,
    private val playerBrawlerHyperChargeJpaRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val playerBrawlerBuffyJpaRepository: PlayerBrawlerBuffyJpaRepository,
) {
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): Player? {
        val entity = playerJpaRepository.findWithLockByBrawlStarsTag(brawlStarsTag) ?: return null
        val brawlers = playerBrawlerJpaRepository.findAllByPlayer(entity)
        if (brawlers.isEmpty()) {
            return Player(entity = entity)
        }
        return Player(
            entity = entity,
            brawlers = brawlers,
            gears = playerBrawlerGearJpaRepository.findAllByPlayerBrawlerIn(brawlers),
            starPowers = playerBrawlerStarPowerJpaRepository.findAllByPlayerBrawlerIn(brawlers),
            gadgets = playerBrawlerGadgetJpaRepository.findAllByPlayerBrawlerIn(brawlers),
            hyperCharges = playerBrawlerHyperChargeJpaRepository.findAllByPlayerBrawlerIn(brawlers),
            buffys = playerBrawlerBuffyJpaRepository.findAllByPlayerBrawlerIn(brawlers),
        )
    }

    fun save(player: Player): Player {
        playerJpaRepository.save(player.entity)
        playerBrawlerJpaRepository.saveAll(player.brawlers)
        playerBrawlerGearJpaRepository.saveAll(player.gears)
        playerBrawlerStarPowerJpaRepository.saveAll(player.starPowers)
        playerBrawlerGadgetJpaRepository.saveAll(player.gadgets)
        playerBrawlerHyperChargeJpaRepository.saveAll(player.hyperCharges)
        playerBrawlerBuffyJpaRepository.saveAll(player.buffys)
        return player
    }
}
