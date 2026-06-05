package com.imstargg.infrastructure.db.core.brawlstars.event

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "battle_event_mode",
    indexes = [
        Index(name = "uk_battle_event_mode__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BattleEventModeEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "brawlstars_name", updatable = false, nullable = false)
    val brawlStarsName: String,
    resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
    playerCounts: List<Int> = DEFAULT_PLAYER_COUNTS,
    color: String = DEFAULT_COLOR,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_event_mode_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    var resultType: BattleEventModeResultType = resultType
        protected set

    /**
     * 각 팀의 인원 수 리스트. 결과형 통계의 픽률 계산에서 게임 수 도출에 사용된다.
     *
     * 예시:
     *  - 3v3: [3, 3]
     *  - Solo Showdown: [1, 1, 1, 1, 1, 1, 1, 1, 1, 1] (10팀 × 1명)
     *  - Duels (1v1): [1, 1]
     *  - 비대칭(가상): [1, 5]
     *
     * Rank 모드(`BattleEventModeResultType.RANK`)에서도 의미는 동일하지만,
     * Rank 통계 엔티티(`BrawlerBattleRankStatsEntity`)는 본인 팀 슬롯만 집계하므로
     * 이 값으로 게임 수를 환산할 수 없다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "player_counts", nullable = false)
    var playerCounts: List<Int> = playerCounts
        protected set

    /**
     * 이벤트 카드 배경에 쓰이는 모드 대표색 (`#RRGGBB`).
     * 표시 메타데이터(이름·아이콘)와 함께 DB 가 단일 출처이며, 관리자 화면에서 편집한다.
     * 색상이 지정되지 않은 모드는 [DEFAULT_COLOR] 회색으로 표시된다.
     */
    @Column(name = "color", nullable = false)
    var color: String = color
        protected set

    init {
        validatePlayerCounts(playerCounts)
        validateColor(color)
    }

    fun updateResultType(newType: BattleEventModeResultType) {
        this.resultType = newType
    }

    fun updatePlayerCounts(newCounts: List<Int>) {
        validatePlayerCounts(newCounts)
        this.playerCounts = newCounts
    }

    fun updateColor(newColor: String) {
        validateColor(newColor)
        this.color = newColor
    }

    private fun validatePlayerCounts(counts: List<Int>) {
        require(counts.isNotEmpty()) { "playerCounts must not be empty" }
        require(counts.all { it >= 1 }) { "playerCounts: every team size must be >= 1, but got $counts" }
    }

    private fun validateColor(value: String) {
        require(COLOR_REGEX.matches(value)) { "color must be #RRGGBB hex, but got $value" }
    }

    companion object {
        // API 응답에서 name이 null인 경우 사용
        const val UNKNOWN_NAME = "UNKNOWN"

        /** 절대다수의 모드가 3v3 이므로 기본값. 마이그레이션과 일치. */
        val DEFAULT_PLAYER_COUNTS: List<Int> = listOf(3, 3)

        /** 색상 미지정 모드의 기본 회색. 마이그레이션 default 와 일치. */
        const val DEFAULT_COLOR = "#2A2E38"

        private val COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")
    }
}
