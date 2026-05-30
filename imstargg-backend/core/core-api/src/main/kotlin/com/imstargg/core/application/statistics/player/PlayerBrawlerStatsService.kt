package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.enums.BattleType
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PlayerBrawlerStatsService(
    private val playerBrawlerStatsRepositoryWithCache: PlayerBrawlerStatsRepositoryWithCache,
) {
    fun get(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats =
        playerBrawlerStatsRepositoryWithCache.find(
            playerTag = playerTag,
            battleType = battleType,
            startAt = startAt,
            endAt = endAt,
        )
}
