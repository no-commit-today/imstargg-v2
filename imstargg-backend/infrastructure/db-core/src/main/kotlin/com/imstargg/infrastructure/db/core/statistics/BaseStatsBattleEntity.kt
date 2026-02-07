package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankTier
import com.imstargg.infrastructure.db.core.BaseEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityTeamPlayer
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
    @Column(name = "event_mode", length = 65, updatable = false, nullable = false)
    val eventMode: String,
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
) : BaseEntity() {
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
        fun canCreate(battle: BattleEntity): Boolean {
            val event = battle.event ?: return false

            if (event.brawlStarsId == null || event.mode == null || event.map == null) {
                return false
            }
            if (battle.mode == null || battle.type == null) {
                return false
            }

            val battleType = EnumCodeType.findByCode<BattleType>(battle.type!!) ?: return false
            if (battleType == BattleType.FRIENDLY) {
                return false
            }

            val allPlayers = battle.teams.flatten()

            // 브롤러 ID 중복 검사
            val brawlerIds = allPlayers.map { it.brawler.brawlStarsId }
            if (brawlerIds.size != brawlerIds.distinct().size) {
                return false
            }

            val minTrophies = allPlayers.minOfOrNull { it.brawler.trophies } ?: return false

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
         * teams 변환: BattleEntityTeamPlayer → StatsBattleEntityTeamPlayer
         */
        internal fun convertTeams(teams: List<List<BattleEntityTeamPlayer>>): List<List<StatsBattleEntityTeamPlayer>> =
            teams.map { team ->
                team.map { player -> StatsBattleEntityTeamPlayer.from(player) }
            }
    }
}
