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
 * 패치 기준 통계로 전환하는 동안 남겨 두는 옛 통계다. 2단계에서 지운다.
 *
 * 같은 팀 브롤러 3인 조합(결과형 3v3 한 팀 전체)의 승/패/무 배틀 통계.
 *
 * ## 한 행의 뜻
 * (티어, 주 버킷)에서 이 조합이 성립한 팀의 승·패·무.
 * 티어는 멤버 전원 기준이다 — 팀 멤버 중 최저 trophies 로 판정한다.
 * 조합은 순서가 없으므로 `brawler1BrawlStarsId < brawler2BrawlStarsId < brawler3BrawlStarsId` 로 정렬한 1행으로 저장한다.
 *
 * ## 이벤트 키를 뺀 이유
 * 2026-11 스펙 B 에서 이벤트 키를 뺐다. 이벤트(맵)에 따른 승률 차이는 대부분 개인 브롤러의 1인 맵 효과로 설명되고,
 * 이벤트별 값은 표본만 얇게 흩어 예측에 보탬이 없었다.
 *
 * ## 물리 구조
 * `start_battle_date` 월 범위 파티션 테이블이다. PK `(id, start_battle_date)`·파티션·CHECK 제약은 `schema.sql` 에만 있고,
 * 이 엔티티는 단일 `id` 를 `@Id` 로 쓴다. 정렬 순서는 `init` 의 `require` 로도 지킨다.
 */
@Entity
@Table(
    name = "brawler_trio_battle_result_stats_backup",
    indexes = [
        Index(
            name = "ix_brawler_trio_battle_result_stats_backup__brawler1",
            columnList = "tier_range, brawler1_brawlstars_id, start_battle_date",
        ),
        Index(
            name = "ix_brawler_trio_battle_result_stats_backup__brawler2",
            columnList = "tier_range, brawler2_brawlstars_id, start_battle_date",
        ),
        Index(
            name = "ix_brawler_trio_battle_result_stats_backup__brawler3",
            columnList = "tier_range, brawler3_brawlstars_id, start_battle_date",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_trio_battle_result_stats_backup__1",
            columnNames = [
                "tier_range",
                "start_battle_date",
                "brawler1_brawlstars_id",
                "brawler2_brawlstars_id",
                "brawler3_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerTrioBattleResultStatsBackupEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
    @Column(name = "brawler1_brawlstars_id", updatable = false, nullable = false)
    val brawler1BrawlStarsId: Long,
    @Column(name = "brawler2_brawlstars_id", updatable = false, nullable = false)
    val brawler2BrawlStarsId: Long,
    @Column(name = "brawler3_brawlstars_id", updatable = false, nullable = false)
    val brawler3BrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(brawler1BrawlStarsId < brawler2BrawlStarsId && brawler2BrawlStarsId < brawler3BrawlStarsId) {
            "brawler1BrawlStarsId($brawler1BrawlStarsId) < brawler2BrawlStarsId($brawler2BrawlStarsId) < " +
                "brawler3BrawlStarsId($brawler3BrawlStarsId) 순서여야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerTrioBattleResultStatsBackupSeq")
    @SequenceGenerator(
        name = "brawlerTrioBattleResultStatsBackupSeq",
        sequenceName = "brawler_trio_battle_result_stats_backup_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_trio_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
