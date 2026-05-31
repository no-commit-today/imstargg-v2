package com.imstargg.infrastructure.redis.core.statistics.player

/**
 * Redis Hash 에 저장되는 플레이어 브롤러 통계 캐시 값.
 * 모든 식별자(playerTag 키 + battleType/startBattleDate/endBattleDate 필드)는 캐시 키에 들어가므로
 * 값에는 담지 않고 items 페이로드만 둔다.
 * Jackson(Kotlin module)만으로 직렬화되도록 모든 필드를 primitive 로 둔다.
 */
data class PlayerBrawlerStatsCacheValue(
    val items: List<Item>,
) {
    data class Item(
        val brawlerId: Long,
        val victoryCount: Long,
        val defeatCount: Long,
        val drawCount: Long,
    )
}
