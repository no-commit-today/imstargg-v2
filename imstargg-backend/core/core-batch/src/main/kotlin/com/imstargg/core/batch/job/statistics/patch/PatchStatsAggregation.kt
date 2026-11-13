package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchStatsAggregator
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity

/** 통계 유형 하나를 집계 잡에 꽂는 묶음. */
class PatchStatsAggregation<TBattle : BaseStatsBattleEntity>(
    val statsType: StatsType,
    val table: PatchStatsTable,
    val reader: StatsBattleReader<TBattle>,
    val createAggregator: () -> PatchStatsAggregator<TBattle>,
)
