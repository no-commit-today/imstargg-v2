package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 순위형(1위~N위) 배틀 통계의 브롤러 단위 집계. 패치마다 키별 한 줄이다.
 *
 * ## 한 줄의 키
 * 패치 × 티어 구간 × 이벤트 × 브롤러.
 *
 * ## 셈 규칙
 * 한 배틀의 **본인 팀 슬롯만** 센다 — 순위형 배틀에는 적군 개념이 없기 때문.
 * 예: Solo Showdown 10팀 1게임은 한 슬롯만 기록된다.
 * 한 게임의 일부 슬롯만 들어가므로 슬롯 합과 팀 구성만으로는 게임 수를 도출할 수 없다.
 *
 * 순위별 횟수는 `rank1_count` 부터 `rank10_count` 까지 컬럼 10개다(배열의 칸별 덧셈을 SQL 로 쓸 수 없어서).
 * `totalCount` 는 순위별 횟수의 합이다. 잡이 SQL 로 직접 쓴다.
 */
@Entity
@Table(
    name = "brawler_battle_rank_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_battle_rank_stats__1",
            columnNames = [
                "brawlstars_patch_id",
                "tier_range",
                "event_brawlstars_id",
                "brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerBattleRankStatsEntity(
    @Column(name = "brawlstars_patch_id", updatable = false, nullable = false)
    val brawlStarsPatchId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    rankCounts: List<Long>,
) : BaseTimeEntity() {
    init {
        require(rankCounts.size == MAX_RANK) {
            "rankCounts 크기(${rankCounts.size})는 $MAX_RANK 이어야 합니다."
        }
    }

    @Column(name = "rank1_count", nullable = false)
    val rank1Count: Long = rankCounts[0]

    @Column(name = "rank2_count", nullable = false)
    val rank2Count: Long = rankCounts[1]

    @Column(name = "rank3_count", nullable = false)
    val rank3Count: Long = rankCounts[2]

    @Column(name = "rank4_count", nullable = false)
    val rank4Count: Long = rankCounts[3]

    @Column(name = "rank5_count", nullable = false)
    val rank5Count: Long = rankCounts[4]

    @Column(name = "rank6_count", nullable = false)
    val rank6Count: Long = rankCounts[5]

    @Column(name = "rank7_count", nullable = false)
    val rank7Count: Long = rankCounts[6]

    @Column(name = "rank8_count", nullable = false)
    val rank8Count: Long = rankCounts[7]

    @Column(name = "rank9_count", nullable = false)
    val rank9Count: Long = rankCounts[8]

    @Column(name = "rank10_count", nullable = false)
    val rank10Count: Long = rankCounts[9]

    @Column(name = "total_count", nullable = false)
    val totalCount: Long = rankCounts.sum()

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_battle_rank_stats_id", nullable = false)
    val id: Long? = null

    /** 순위별 횟수. 0번이 1위다. */
    fun rankCounts(): List<Long> =
        listOf(
            rank1Count,
            rank2Count,
            rank3Count,
            rank4Count,
            rank5Count,
            rank6Count,
            rank7Count,
            rank8Count,
            rank9Count,
            rank10Count,
        )

    companion object {
        const val MAX_RANK = 10
    }
}
