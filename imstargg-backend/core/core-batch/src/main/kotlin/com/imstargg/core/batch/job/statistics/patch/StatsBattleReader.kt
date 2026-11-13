package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import java.time.Instant
import java.time.LocalDate

/** 패치 통계가 읽는 원본 전투. 읽기는 트랜잭션 밖에서 하고, 쪽마다 영속성 컨텍스트를 비운다. */
interface StatsBattleReader<TBattle : BaseStatsBattleEntity> {
    /** startBattleDate 이후 날짜의 원본 중 created_at 이 createdAtCap 이하인 것의 가장 큰 id. 없으면 null. */
    fun findBattleIdCap(
        createdAtCap: Instant,
        startBattleDate: LocalDate,
    ): Long?

    /** 원본에 남아 있는 가장 이른 전투 날짜. 원본이 없으면 null. */
    fun findEarliestBattleDate(): LocalDate?

    /**
     * 전투 날짜가 dateRange 안이고 afterBattleId < id <= battleIdCap 인 원본을 id 순으로, 한 쪽씩 넘긴다.
     *
     * 빈 쪽은 넘기지 않는다(`PatchStatsTask` 와 `PatchStatsRangeAligner` 의 `battles.maxOf` 가 여기에 기댄다).
     */
    fun read(
        dateRange: PatchDateRange,
        afterBattleId: Long,
        battleIdCap: Long,
        consume: (List<TBattle>) -> Unit,
    )
}
