package com.imstargg.infrastructure.redis.core.statistics.player

/**
 * Redis Hash 에 저장되는 플레이어 브롤러 통계 캐시 값.
 * 키(playerTag)·필드(battleType/startBattleDate)는 캐시 키에 들어가므로 값에는 담지 않는다.
 * Jackson(Kotlin module)만으로 직렬화되도록 모든 필드를 primitive 로 둔다(날짜는 ISO-8601 문자열).
 */
data class PlayerBrawlerStatsCacheValue(
    val endBattleDate: String,
    val items: List<Item>,
) {
    data class Item(
        val brawlerId: Long,
        val victoryCount: Long,
        val defeatCount: Long,
        val drawCount: Long,
    )
}
