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
 * 결과형 배틀에서 브롤러가 상대 브롤러를 만났을 때의 승/패/무 통계. 패치마다 키별 한 줄이다.
 *
 * ## 한 줄의 키
 * 패치 × 티어 구간 × 브롤러 × 상대 브롤러.
 *
 * ## 셈 규칙
 * 브롤러와 상대는 서로 다른 팀이다. 브롤러 기준의 승/패/무를 센다.
 * 상대는 자기 자신일 수 없다.
 *
 * 잡이 SQL 로 직접 쓴다.
 */
@Entity
@Table(
    name = "brawler_enemy_battle_result_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_enemy_battle_result_stats__1",
            columnNames = [
                "brawlstars_patch_id",
                "tier_range",
                "brawler_brawlstars_id",
                "enemy_brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerEnemyBattleResultStatsEntity(
    @Column(name = "brawlstars_patch_id", updatable = false, nullable = false)
    val brawlStarsPatchId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "enemy_brawler_brawlstars_id", updatable = false, nullable = false)
    val enemyBrawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(brawlerBrawlStarsId != enemyBrawlerBrawlStarsId) {
            "enemyBrawlerBrawlStarsId($enemyBrawlerBrawlStarsId)는 brawlerBrawlStarsId($brawlerBrawlStarsId)와 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_enemy_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
