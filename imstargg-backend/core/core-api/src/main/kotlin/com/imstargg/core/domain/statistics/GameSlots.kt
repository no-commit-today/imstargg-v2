package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 이벤트별 [GameSlot] 묶음. 등록되지 않은 eventId 는 [GameSlot.default] 로 폴백한다.
 */
class GameSlots(
    private val byEventId: Map<BrawlStarsId, GameSlot>,
) {
    operator fun get(eventId: BrawlStarsId): GameSlot = byEventId[eventId] ?: GameSlot.default()
}
