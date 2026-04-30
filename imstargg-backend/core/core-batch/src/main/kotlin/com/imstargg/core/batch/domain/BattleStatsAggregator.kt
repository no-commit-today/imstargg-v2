package com.imstargg.core.batch.domain

/**
 * 배틀 데이터를 통계 엔티티로 집계하는 인터페이스.
 *
 * ## 집계 흐름
 * 1. [aggregate]를 반복 호출하여 배틀 데이터를 누적
 * 2. [toEntities]로 누적된 결과를 통계 엔티티 목록으로 변환
 *
 * ## 공통 집계 규칙
 * - **집계 키 차원**: 이벤트(맵) × 티어 구간 × 날짜 + Aggregator별 추가 차원
 * - **티어 구간(TierRange)**: [StatsTierRange.resolveAll]로 결정. 배틀 타입과 트로피 수에 따라
 *   해당하는 모든 누적 티어 구간을 반환하므로, 하나의 배틀이 여러 티어 구간에 동시에 집계될 수 있음
 * - **알 수 없는 배틀 타입/결과**: 파싱 불가능한 경우 해당 배틀을 건너뜀
 */
interface BattleStatsAggregator<TBattle, TEntity> {
    fun aggregate(battle: TBattle)

    fun forEachEntity(consumer: (TEntity) -> Unit) {
        toEntities().forEach(consumer)
    }

    fun flushEntityChunks(
        chunkSize: Int,
        flush: (List<TEntity>) -> Unit,
    ) {
        require(chunkSize > 0) { "chunkSize must be greater than 0" }

        val buffer = ArrayList<TEntity>(chunkSize)
        forEachEntity { entity ->
            buffer.add(entity)
            if (buffer.size == chunkSize) {
                flush(buffer.toList())
                buffer.clear()
            }
        }

        if (buffer.isNotEmpty()) {
            flush(buffer.toList())
        }
    }

    fun toEntities(): List<TEntity>
}
