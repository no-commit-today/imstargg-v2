package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.domain.brawlstars.player.PlayerSearch
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
) {
    fun get(tag: BrawlStarsTag): Player =
        playerRepository.find(tag)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun getPlayerBrawlers(tag: BrawlStarsTag): List<PlayerBrawler> = playerRepository.findAllPlayerBrawler(tag)

    fun search(
        search: PlayerSearch,
        offsetLimit: OffsetLimit,
    ): Slice<Player> =
        if (BrawlStarsTag.isValid(search.query)) {
            val player = playerRepository.searchByTag(BrawlStarsTag(search.query))
            Slice(content = listOfNotNull(player), hasNext = false)
        } else {
            playerRepository.searchByName(search.query, offsetLimit)
        }
}
