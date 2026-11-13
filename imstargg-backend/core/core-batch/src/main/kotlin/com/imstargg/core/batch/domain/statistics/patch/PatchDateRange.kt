package com.imstargg.core.batch.domain.statistics.patch

import java.time.LocalDate

/**
 * 한 패치가 차지하는 전투 날짜의 범위(KST 날짜, 양 끝 포함).
 *
 * 패치일 다음 날부터 다음 패치일 전날까지다. [endDate] 가 null 이면 끝이 없다.
 */
data class PatchDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate?,
) {
    /** 끝이 있고 시작보다 이르면 비어 있다. 패치일이 연이은 날짜일 때 앞 패치가 그렇다. */
    val isEmpty: Boolean
        get() = endDate != null && endDate < startDate

    fun contains(date: LocalDate): Boolean = date >= startDate && (endDate == null || date <= endDate)

    companion object {
        /**
         * 패치 id → 패치일을 받아 패치 id → 범위를 준다. 범위가 빈 패치도 들어 있다.
         * 패치 당일과 가장 이른 패치 이전의 날짜는 어느 범위에도 들지 않는다.
         */
        fun resolveAll(patchDates: Map<Long, LocalDate>): Map<Long, PatchDateRange> {
            val sorted = patchDates.entries.sortedBy { it.value }
            return sorted.indices.associate { i ->
                val next = sorted.getOrNull(i + 1)
                sorted[i].key to
                    PatchDateRange(
                        startDate = sorted[i].value.plusDays(1),
                        endDate = next?.value?.minusDays(1),
                    )
            }
        }
    }
}
