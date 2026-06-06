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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 결과형(승/패/무) 배틀 통계의 브롤러 단위 집계.
 *
 * ## 집계 단위
 * 브롤러 × 이벤트 × 티어 구간 × 날짜.
 *
 * ## 집계 범위
 * 한 배틀의 **양 팀 모든 슬롯**이 카운트된다 — 자기 팀은 본래 결과로, 적군 팀은 반대 결과로.
 * 3v3 의 경우 한 게임당 6 슬롯이 다수 row 에 분산 기록된다.
 *
 * ## 게임 수 도출
 * `BattleEventMode.playerCounts` 가 알려진다면 게임 수를 정확히 도출할 수 있다:
 *
 *     eventSlotSum = Σ totalCount over brawlers within event
 *     gameCount    = eventSlotSum / GameSlot(playerCounts).resultSlotsPerGame
 *
 * 픽률 (= 브롤러 슬롯 수 / 게임 수) 계산이 이 관계에 의존한다.
 * `BrawlerBattleRankStatsEntity` 와의 결정적 차이는 이 엔티티가 양 팀을 모두 집계한다는 점이다.
 */
@Entity
@Table(
    name = "brawler_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, battle_date",
        ),
        Index(
            name = "ix_brawler_battle_result_stats_2",
            columnList = "brawler_brawlstars_id, tier_range, battle_date",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_battle_result_stats__1",
            columnNames = [
                "event_brawlstars_id",
                "tier_range",
                "battle_date",
                "brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerBattleResultStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
    @Column(name = "star_player_count", updatable = false, nullable = false)
    val starPlayerCount: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
