package com.imstargg.core.batch.job.statistics.patch

import java.time.Instant
import java.time.LocalDate

/** 체크포인트 줄을 고칠 값. version 은 줄을 읽었을 때의 수정 번호다. */
data class StatsCheckpointUpdate(
    val checkpointId: Long,
    val version: Long,
    val lastReadBattleId: Long,
    val appliedStartDate: LocalDate,
    val appliedEndDate: LocalDate?,
    val lastBattleAt: Instant?,
)
