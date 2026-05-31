package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheStore
import com.imstargg.infrastructure.redis.core.statistics.player.PlayerBrawlerStatsCacheValue
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 모듈 간(core-api 읽기/쓰기, core-worker 무효화) 키 공유를 위해 Spring @Cacheable 대신
 * redis-core 의 명시적 캐시 스토어로 cache-aside 한다. (기존 *WithCache 패턴의 의도된 변형)
 */
@Repository
class PlayerBrawlerStatsRepositoryWithCache(
    private val playerBrawlerStatsRepository: PlayerBrawlerStatsRepository,
    private val cacheStore: PlayerBrawlerStatsCacheStore,
) {
    fun find(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats {
        val startBattleDate = startAt.toLocalDate()
        val endBattleDate = endAt.toLocalDate()
        cacheStore
            .find(
                playerTag = playerTag.value,
                battleType = battleType,
                startBattleDate = startBattleDate,
                endBattleDate = endBattleDate,
            )?.let { cached ->
                return cached.toDomain(
                    playerTag = playerTag,
                    battleType = battleType,
                    startBattleDate = startBattleDate,
                    endBattleDate = endBattleDate,
                )
            }
        val computed =
            playerBrawlerStatsRepository.compute(
                playerTag = playerTag,
                battleType = battleType,
                startAt = startAt,
                endAt = endAt,
            )
        cacheStore.put(
            playerTag = playerTag.value,
            battleType = battleType,
            startBattleDate = startBattleDate,
            endBattleDate = endBattleDate,
            value = computed.toCacheValue(),
        )
        return computed
    }
}

private fun PlayerBrawlerStatsCacheValue.toDomain(
    playerTag: BrawlStarsTag,
    battleType: BattleType,
    startBattleDate: LocalDate,
    endBattleDate: LocalDate,
): PlayerBrawlerStats =
    PlayerBrawlerStats(
        playerTag = playerTag,
        battleType = battleType,
        startBattleDate = startBattleDate,
        endBattleDate = endBattleDate,
        items =
            items.map { item ->
                PlayerBrawlerStatsItem(
                    brawlerId = BrawlStarsId(item.brawlerId),
                    resultCount = ResultCount(item.victoryCount, item.defeatCount, item.drawCount),
                )
            },
    )

private fun PlayerBrawlerStats.toCacheValue(): PlayerBrawlerStatsCacheValue =
    PlayerBrawlerStatsCacheValue(
        items =
            items.map { item ->
                PlayerBrawlerStatsCacheValue.Item(
                    brawlerId = item.brawlerId.value,
                    victoryCount = item.resultCount.victoryCount,
                    defeatCount = item.resultCount.defeatCount,
                    drawCount = item.resultCount.drawCount,
                )
            },
    )
