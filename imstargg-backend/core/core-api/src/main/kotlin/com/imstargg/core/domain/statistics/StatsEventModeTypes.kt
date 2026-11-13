package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.StatsEventModeType

/**
 * 통계를 수집하는 이벤트 모드와 그 수집 유형의 목록.
 * 목록에 없는 모드는 통계를 모으지 않는다.
 */
class StatsEventModeTypes(
    private val typesByModeId: Map<BrawlStarsId, StatsEventModeType>,
) {
    /** 모드의 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
    fun resultTypeOf(modeId: BrawlStarsId): BattleEventModeResultType? = typesByModeId[modeId]?.resultType
}
