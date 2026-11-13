package com.imstargg.core.batch.job.statistics.patch

/**
 * 통계 테이블 하나의 모양. keyColumns 에 brawlstars_patch_id 는 넣지 않는다.
 *
 * [keyColumns] 와 [countColumns] 의 순서는 [com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount] 의
 * keys·counts 순서와 같다. [countColumns] 에는 `total_count` 가 있어야 한다(합이 0 인 줄을 지울 때 쓴다).
 */
class PatchStatsTable(
    val name: String,
    val keyColumns: List<String>,
    val countColumns: List<String>,
)
