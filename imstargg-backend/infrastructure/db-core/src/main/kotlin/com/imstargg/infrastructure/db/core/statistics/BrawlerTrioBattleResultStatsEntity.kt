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

/**
 * 같은 팀 브롤러 3인 조합(결과형 3v3 한 팀 전체)의 승/패/무 통계. 패치마다 키별 한 줄이다.
 *
 * ## 한 줄의 키
 * 패치 × 티어 구간 × 브롤러 셋. 티어는 멤버 전원 기준이다(팀 멤버 중 최저 trophies 로 판정).
 * 조합은 순서가 없으므로 `brawler1BrawlStarsId < brawler2BrawlStarsId < brawler3BrawlStarsId` 로 정렬한 한 줄로 저장한다.
 * 브롤러는 어느 자리에든 있을 수 있어 둘째·셋째 브롤러에도 인덱스를 둔다.
 *
 * 잡이 SQL 로 직접 쓴다.
 */
@Entity
@Table(
    name = "brawler_trio_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_trio_battle_result_stats__brawler2",
            columnList = "brawlstars_patch_id, tier_range, brawler2_brawlstars_id",
        ),
        Index(
            name = "ix_brawler_trio_battle_result_stats__brawler3",
            columnList = "brawlstars_patch_id, tier_range, brawler3_brawlstars_id",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_trio_battle_result_stats__1",
            columnNames = [
                "brawlstars_patch_id",
                "tier_range",
                "brawler1_brawlstars_id",
                "brawler2_brawlstars_id",
                "brawler3_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerTrioBattleResultStatsEntity(
    @Column(name = "brawlstars_patch_id", updatable = false, nullable = false)
    val brawlStarsPatchId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_trio_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
