package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsType
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
import java.time.Instant
import java.time.LocalDate

/**
 * 패치별 통계의 체크포인트. 한 줄이 통계 유형 × 패치다.
 *
 * ## 지키는 규칙
 * 한 패치의 통계는 "전투 날짜가 적용한 범위(`appliedStartDate` ~ `appliedEndDate`) 안이고
 * id 가 `lastReadBattleId` 이하"인 원본 전투를 집계한 값과 같다.
 * 모든 쓰기 트랜잭션이 통계 줄과 체크포인트 줄을 함께 바꾸므로 잡이 어디서 죽어도 이 규칙이 성립한다.
 *
 * ## 값의 뜻
 * - `lastReadBattleId`: 1인 순위는 순위형 원본의 id, 나머지 넷은 결과형 원본의 id.
 * - `appliedEndDate`: 다음 패치가 없으면 비운다(끝 없음).
 * - `lastBattleAt`: 반영된 가장 최근 전투 일시. 반영된 전투가 없으면 비어 있다.
 * - `version`: 수정 번호. 쓸 때마다 1 올린다. `@Version` 이 아니라 잡의 조건부 SQL 이 쓴다.
 *
 * 잡이 SQL 로 직접 쓴다.
 */
@Entity
@Table(
    name = "stats_checkpoint",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_stats_checkpoint__stats_type_brawlstars_patch_id",
            columnNames = ["stats_type", "brawlstars_patch_id"],
        ),
    ],
)
class StatsCheckpointEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "stats_type", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val statsType: StatsType,
    @Column(name = "brawlstars_patch_id", updatable = false, nullable = false)
    val brawlStarsPatchId: Long,
    @Column(name = "last_read_battle_id", nullable = false)
    val lastReadBattleId: Long,
    @Column(name = "applied_start_date", nullable = false)
    val appliedStartDate: LocalDate,
    @Column(name = "applied_end_date")
    val appliedEndDate: LocalDate?,
    @Column(name = "last_battle_at")
    val lastBattleAt: Instant?,
    @Column(name = "version", nullable = false)
    val version: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_checkpoint_id", nullable = false)
    val id: Long? = null
}
