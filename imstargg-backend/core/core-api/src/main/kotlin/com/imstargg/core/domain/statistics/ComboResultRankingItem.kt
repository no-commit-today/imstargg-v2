package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 전체 2인·3인 조합 정렬의 항목 하나. [PairResultRanking]·[TrioResultRanking] 이 함께 쓴다.
 */
data class ComboResultRankingItem(
    /** 조합의 브롤러들(id 오름차순) — 2인은 `[brawler1, brawler2]`, 3인은 `[brawler1, brawler2, brawler3]`. */
    val brawlerIds: List<BrawlStarsId>,
    /** 이 조합이 성립한 팀 수(= 조합 행의 `total_count` 합). */
    val battleCount: Long,
    val pickRate: Double,
    /** 함께일 때 승률. */
    val winRate: Double,
    val tierScore: TierScore,
)
