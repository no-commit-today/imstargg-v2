package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 이벤트별 [GameSlot] 묶음. 등록되지 않은 eventId 는 [GameSlot.default] 로 폴백한다.
 */
class GameSlots(
    private val byEventId: Map<BrawlStarsId, GameSlot>,
) {
    operator fun get(eventId: BrawlStarsId): GameSlot = byEventId[eventId] ?: GameSlot.default()

    /**
     * 조회 범위의 총 게임 수 = Σ (이벤트별 Result 슬롯 합 ÷ [GameSlot.resultSlotsPerGame]).
     *
     * Pair / Enemy 통계의 분모도 동일한 게임 수를 사용하므로 항상 Result 슬롯 합 기준으로 계산한다.
     * 수학적으로 `pair_slot/pairSlotsPerGame == enemy_slot/enemySlotsPerGame == result_slot/resultSlotsPerGame == gameCount`
     * 가 보장되므로 Result 테이블을 단일 진실 출처로 사용한다.
     * 분모 의미 정정 이력은 [StatsRate.pickRate] 참조.
     */
    fun totalGameCount(resultSlotSumByEventId: Map<BrawlStarsId, Long>): Long =
        resultSlotSumByEventId.entries.sumOf { (eventId, slotSum) ->
            slotSum / this[eventId].resultSlotsPerGame
        }
}
