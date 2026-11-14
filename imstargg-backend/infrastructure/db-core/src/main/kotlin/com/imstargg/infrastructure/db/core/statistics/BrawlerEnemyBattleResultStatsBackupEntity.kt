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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 패치 기준 통계로 전환하는 동안 남겨 두는 옛 통계다. 2단계에서 지운다.
 *
 * 브롤러 vs 적 브롤러(카운터) 결과형(승/패/무) 배틀 통계.
 *
 * ## 한 행의 뜻
 * (티어, 주 버킷)에서 `brawler` 가 `enemy` 를 상대로 만난 결과(`brawler` 기준).
 * (X, Y) 행과 (Y, X) 행은 방향이 있는 별개 행이며, 승↔패가 서로 보완된다.
 * 티어는 기준 플레이어(`brawler`) 의 trophies 로 판정한다.
 *
 * ## 이벤트 키를 뺀 이유
 * 2026-11 스펙 B 에서 이벤트 키를 뺐다. 이벤트(맵)에 따른 승률 차이는 대부분 개인 브롤러의 1인 맵 효과로 설명되고,
 * 이벤트별 값은 표본만 얇게 흩어 예측에 보탬이 없었다.
 *
 * ## 물리 구조
 * `start_battle_date` 월 범위 파티션 테이블이다. PK `(id, start_battle_date)`·파티션·CHECK 제약은 `schema.sql` 에만 있고,
 * 이 엔티티는 단일 `id` 를 `@Id` 로 쓴다. 유니크 인덱스가 `brawler` 기준 조회를 겸하므로 별도 조회 인덱스는 없다.
 */
@Entity
@Table(
    name = "brawler_enemy_battle_result_stats_backup",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_enemy_battle_result_stats_backup__1",
            columnNames = [
                "tier_range",
                "brawler_brawlstars_id",
                "start_battle_date",
                "enemy_brawler_brawlstars_id",
            ],
        ),
    ],
)
class BrawlerEnemyBattleResultStatsBackupEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "enemy_brawler_brawlstars_id", updatable = false, nullable = false)
    val enemyBrawlerBrawlStarsId: Long,
    @Embedded
    val resultCount: ResultCountEmbedded,
) : BaseTimeEntity() {
    init {
        require(brawlerBrawlStarsId != enemyBrawlerBrawlStarsId) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)와 enemyBrawlerBrawlStarsId($enemyBrawlerBrawlStarsId)는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerEnemyBattleResultStatsBackupSeq")
    @SequenceGenerator(
        name = "brawlerEnemyBattleResultStatsBackupSeq",
        sequenceName = "brawler_enemy_battle_result_stats_backup_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_enemy_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
