package com.imstargg.core.batch.domain.statistics.patch

/**
 * 통계 줄 하나의 키와 더하거나 뺄 값.
 *
 * [keys] 와 [counts] 의 순서는 통계 테이블의 키 컬럼·값 컬럼 순서다. 패치는 들어 있지 않다.
 * 티어 구간은 [com.imstargg.core.enums.StatsTierRange.name], 브롤러·이벤트 id 는 [Long] 이다.
 */
data class PatchStatsCount(
    val keys: List<Any>,
    val counts: List<Long>,
)
