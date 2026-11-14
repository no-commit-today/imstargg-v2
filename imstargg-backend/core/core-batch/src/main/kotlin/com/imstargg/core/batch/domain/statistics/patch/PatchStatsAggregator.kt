package com.imstargg.core.batch.domain.statistics.patch

/**
 * 전투를 패치 통계의 키별 값으로 모은다. 묶음(저장 단위 하나, 또는 하루) 하나마다 새로 만든다.
 *
 * ## 공통 집계 규칙
 * - **집계 키**: 티어 구간 + 집계기별 차원. 패치와 날짜는 키에 없다(호출하는 쪽이 패치별로 묶음을 나눈다)
 * - **티어 구간**: [com.imstargg.core.enums.StatsTierRange.resolveAll] 로 정한다. 하나의 전투가 여러 티어 구간에 동시에 집계될 수 있다
 * - **알 수 없는 전투 유형/결과**: 파싱할 수 없으면 그 전투를 건너뛴다
 * - **봇 전투**: 봇 전투([com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy])는 전투 단위로 건너뛴다.
 *   티어 구간을 정하기 전이라 어느 티어 줄에도 들지 않는다
 */
interface PatchStatsAggregator<TBattle> {
    fun aggregate(battle: TBattle)

    fun toCounts(): List<PatchStatsCount>
}
