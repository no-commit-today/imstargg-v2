package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerEnemyBattleResultStatsBackupJpaRepositoryCustom {
    /**
     * 기준 브롤러(`brawler`)의 상대 브롤러별 결과를, 기간 안 버킷을 합산해 돌려준다.
     */
    fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerEnemyResultProjection>
}
