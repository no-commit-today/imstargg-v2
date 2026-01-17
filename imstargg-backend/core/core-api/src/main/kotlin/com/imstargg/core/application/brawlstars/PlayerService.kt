package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
) {
    fun get(tag: BrawlStarsTag): Player = playerRepository.find(tag)
        ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun getPlayerBrawlers(tag: BrawlStarsTag): List<PlayerBrawler> = playerRepository.findAllPlayerBrawler(tag)
}
