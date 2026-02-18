package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankTier
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleTeamPlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.toTeams
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.OffsetDateTime

@MappedSuperclass
abstract class BaseStatsBattleEntity(
    @Column(name = "created_date", updatable = false, nullable = false)
    val createdDate: LocalDate,
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: OffsetDateTime,
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Column(name = "event_mode_brawlstars_id", updatable = false, nullable = false)
    val eventModeBrawlStarsId: Long,
    @Column(name = "event_map", length = 65, updatable = false, nullable = false)
    val eventMap: String,
    @Column(name = "mode", length = 65, updatable = false, nullable = false)
    val mode: String,
    @Column(name = "type", length = 65, updatable = false, nullable = false)
    val type: String,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseTimeEntity() {
    companion object {
        private const val MIN_TROPHY = 500
        private val MIN_SOLO_RANK_TIER = SoloRankTier.DIAMOND_1

        /**
         * 공통 검증: 모든 Stats Battle 엔티티에 적용
         * - event 필드 (brawlStarsId, mode, map) not null
         * - mode, type not null
         * - type != FRIENDLY
         * - 모든 팀 플레이어 브롤러 트로피 >= 500
         * - SOLO_RANKED인 경우 트로피(SoloRankTier 값) >= DIAMOND_1
         * - 브롤러 ID 중복 없음
         */
        fun canCreate(
            battle: BattleEntity,
            teamPlayers: List<BattleTeamPlayerEntity>,
        ): Boolean {
            require(teamPlayers.all { it.battle === battle }) {
                "teamPlayers의 battle이 인자의 battle과 일치하지 않습니다." +
                    " battleId=${battle.id}, teamPlayerIds=${teamPlayers.map { it.id }}"
            }

            val event = battle.event ?: return false

            if (event.brawlStarsId == null || event.modeBrawlStarsId == null || event.map == null) {
                return false
            }
            if (battle.mode == null || battle.type == null) {
                return false
            }

            val battleType = EnumCodeType.findByCode<BattleType>(battle.type!!) ?: return false
            if (battleType == BattleType.FRIENDLY) {
                return false
            }

            // 브롤러 ID 중복 검사
            val brawlerIds = teamPlayers.map { it.brawler.brawlStarsId }
            if (brawlerIds.size != brawlerIds.distinct().size) {
                return false
            }

            val minTrophies = teamPlayers.minOfOrNull { it.brawler.trophies } ?: return false

            if (battleType == BattleType.SOLO_RANKED) {
                if (minTrophies < MIN_SOLO_RANK_TIER.value) {
                    return false
                }
            } else {
                if (minTrophies < MIN_TROPHY) {
                    return false
                }
            }

            return true
        }

        /**
         * teams 변환: BattleTeamPlayerEntity → StatsBattleEntityTeamPlayer
         */
        internal fun convertTeams(teamPlayers: List<BattleTeamPlayerEntity>): List<List<StatsBattleEntityTeamPlayer>> =
            teamPlayers.toTeams().map { team ->
                team.map { player -> StatsBattleEntityTeamPlayer.from(player) }
            }
    }
}
