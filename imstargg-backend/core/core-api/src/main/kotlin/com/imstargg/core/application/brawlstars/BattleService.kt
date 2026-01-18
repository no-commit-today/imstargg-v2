package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.battle.Battle
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class BattleService(
    private val playerRepository: PlayerRepository,
    private val battleRepository: BattleRepository,
) {
    fun getBattles(
        tag: BrawlStarsTag,
        offsetLimit: OffsetLimit,
    ): Slice<Battle> {
        val player =
            playerRepository.find(tag)
                ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

        return battleRepository.find(player, offsetLimit)
    }
}
