package com.imstargg.core.domain.statistics

/**
 * 조합(2인·3인)·카운터 통계의 최소 표본과 응답 상위 N.
 *
 * | 목록 | 하한 | 상위 N | 정렬 |
 * |---|---|---|---|
 * | 브롤러 기준 2인·3인 | [BRAWLER_MIN_BATTLE_COUNT] | [BRAWLER_TOP_N] | `tierScore` 내림차순 |
 * | 브롤러 기준 카운터 | [BRAWLER_MIN_BATTLE_COUNT] | [BRAWLER_TOP_N] | `winRate` 오름차순 |
 * | 전체 2인·3인 정렬 | [RANKING_MIN_BATTLE_COUNT] | [RANKING_TOP_N] | `tierScore` 내림차순 |
 *
 * ## 근거 (2026-11 스펙 B 결정 9, 부록 B.1)
 * - 하한: 이전 이벤트 패널은 승률순 상위 10 중 30판 미만이 판수 가중 56~86%(10판 미만 35~57%)였다.
 *   얇은 표본이 승률 극단값으로 위에 올라오므로, 30판 미만은 목록에서 뺀다.
 * - 상위 N: 3인 조합은 상한이 없으면 Redis 캐시 항목 하나가 최대 약 1.3MB 다. 캐시에는 하한·상위 N 을
 *   적용한 결과만 저장한다. 2인·카운터는 짝이 107개 이하라 실제로는 상한에 걸리지 않는다.
 * - 상위 티어는 하한을 넘는 조합이 적어 빈 목록이 날 수 있다. 값을 바꿀 때는 캐시 키 버전도 올린다.
 */
object ComboStatsPolicy {
    /** 브롤러 기준 2인·3인·카운터 목록에 남기는 최소 판 수(`battleCount` 이상). */
    const val BRAWLER_MIN_BATTLE_COUNT = 30L

    /** 브롤러 기준 2인·3인·카운터 목록의 최대 항목 수. */
    const val BRAWLER_TOP_N = 200

    /** 전체 2인·3인 조합 정렬에 남기는 최소 판 수(`battleCount` 이상). */
    const val RANKING_MIN_BATTLE_COUNT = 100L

    /** 전체 2인·3인 조합 정렬의 최대 항목 수. */
    const val RANKING_TOP_N = 50

    /**
     * 조합(2인·3인) 티어 스코어의 수축 기준 평균 승률([TierScore.create] 의 `baselineWinRate`).
     *
     * 2×3 결과형에서 조합 평균 승률은 구성상 0.5 다. 한 게임이 이기는 팀의 조합과 지는 팀의 조합을 같은 수만큼
     * 만들기 때문이다. 기본 티어에서는 정확하고, 상위 티어는 전원 기준 티어 판정으로 한쪽 팀만 기록될 수 있어 근사다.
     * X 자기 평균(브롤러 기준 목록)이나 하한 통과 조합 평균(전체 정렬)을 기준으로 삼으면 같은 조합의 등급이
     * 화면마다 달라지므로 전역 기준으로 고정한다. 산식 재검토는 스펙 C.
     */
    const val COMBO_BASELINE_WIN_RATE = 0.5
}
