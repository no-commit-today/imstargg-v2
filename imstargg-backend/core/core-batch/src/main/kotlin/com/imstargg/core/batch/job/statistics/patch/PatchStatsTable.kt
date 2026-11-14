package com.imstargg.core.batch.job.statistics.patch

/**
 * 통계 테이블 하나의 모양. keyColumns 에 brawlstars_patch_id 는 넣지 않는다.
 *
 * [keyColumns] 와 [countColumns] 의 순서는 [com.imstargg.core.batch.domain.statistics.patch.PatchStatsCount] 의
 * keys·counts 순서와 같다. 쓰기 SQL 이 두 컬럼 이름을 글자 그대로 쓰므로 만들 때 확인한다.
 * [keyColumns] 에는 `tier_range` 가(지운 패치의 줄을 티어 구간마다 나눠 지울 때 쓴다),
 * [countColumns] 에는 `total_count` 가 있어야 한다(합이 0 인 줄을 지울 때 쓴다).
 */
class PatchStatsTable(
    val name: String,
    val keyColumns: List<String>,
    val countColumns: List<String>,
) {
    init {
        require("tier_range" in keyColumns) { "키 컬럼에 tier_range 가 없다. name=$name, keyColumns=$keyColumns" }
        require("total_count" in countColumns) { "값 컬럼에 total_count 가 없다. name=$name, countColumns=$countColumns" }
    }
}
