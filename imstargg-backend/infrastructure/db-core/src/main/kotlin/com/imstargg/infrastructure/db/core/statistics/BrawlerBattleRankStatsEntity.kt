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
import jakarta.persistence.Index
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 순위형(1위~N위) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **본인 팀 슬롯만** 카운트된다 — 순위형 배틀에는 적군 개념이 없기 때문.
 * 예: Solo Showdown 10팀 1게임은 통계 entity 상 단 1 슬롯만 기록된다.
 *
 * ## 게임 수 정확 계산 불가
 * Result 통계와 달리 한 게임의 **일부 슬롯만** 통계에 들어가므로
 * `BattleEventMode.playerCounts` 만으로는 게임 수를 도출할 수 없다.
 *
 * 따라서 결과형 통계(`BrawlerResultStatistics` 등)에 도입된
 * "게임 수 분모 픽률" 정의를 이 엔티티에 그대로 옮길 수 없다.
 * Rank 통계용 픽률을 도입하려면 별도의 게임 수 집계 메커니즘이 필요하다 (2026-05-19 결정).
 */
@Entity
@Table(
    name = "brawler_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_battle_rank_stats",
            columnList = "event_brawlstars_id, tier_range, battle_date",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_battle_rank_stats__1",
            columnNames = [
                "event_brawlstars_id",
                "tier_range",
                "battle_date",
                "brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerBattleRankStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    rankCounts: RankCountsEmbedded,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerBattleRankStatsSeq")
    @SequenceGenerator(
        name = "brawlerBattleRankStatsSeq",
        sequenceName = "brawler_battle_rank_stats_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_battle_rank_stats_id", nullable = false)
    val id: Long? = null

    @Embedded
    var rankCounts: RankCountsEmbedded = rankCounts
        protected set
}
