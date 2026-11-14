package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 결과형(승/패/무) 배틀 통계의 브롤러 단위 집계. 패치마다 키별 한 줄이다.
 *
 * ## 한 줄의 키
 * 패치 × 티어 구간 × 이벤트 × 브롤러.
 *
 * ## 셈 규칙
 * 한 배틀의 **양 팀 모든 슬롯**이 센다 — 자기 팀은 본래 결과로, 적군 팀은 반대 결과로.
 * 결과형 지원 모드는 모두 2×3 이라 게임 수 = 슬롯 합 ÷ `StatsEventModeType.RESULT_SLOTS_PER_GAME` 이다.
 *
 * 잡이 SQL 로 직접 쓴다.
 */
@Entity
@Table(
    name = "brawler_battle_result_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_battle_result_stats__1",
            columnNames = [
                "brawlstars_patch_id",
                "tier_range",
                "event_brawlstars_id",
                "brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerBattleResultStatsEntity(
    @Column(name = "brawlstars_patch_id", updatable = false, nullable = false)
    val brawlStarsPatchId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
    @Column(name = "star_player_count", nullable = false)
    val starPlayerCount: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
