package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.StatsBotMonitor
import com.imstargg.core.batch.domain.statistics.patch.PatchStatsAggregator
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity

/** 통계 유형 하나를 집계 잡에 꽂는 묶음. */
class PatchStatsAggregation<TBattle : BaseStatsBattleEntity>(
    val statsType: StatsType,
    /** 봇 감시 알림 제목에 쓰는 잡 이름. */
    val jobName: String,
    val table: PatchStatsTable,
    val reader: StatsBattleReader<TBattle>,
    val createAggregator: () -> PatchStatsAggregator<TBattle>,
    /** 봇 감시를 만드는 자리. null 이면 그 유형은 감시하지 않는다. */
    val createBotMonitor: (() -> StatsBotMonitor<TBattle>)? = null,
)
