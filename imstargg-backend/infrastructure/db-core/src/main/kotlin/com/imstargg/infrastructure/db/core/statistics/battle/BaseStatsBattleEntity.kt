package com.imstargg.infrastructure.db.core.statistics.battle

import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.toKoreaLocalDate
import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate

@MappedSuperclass
abstract class BaseStatsBattleEntity(
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: Instant,
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
    @Column(name = "player_brawlstars_tag", length = 45, updatable = false, nullable = false)
    val playerBrawlStarsTag: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", updatable = false, nullable = false)
    val teams: List<List<StatsBattleEntityTeamPlayer>>,
) : BaseTimeEntity() {
    @Column(name = "battle_date", updatable = false, nullable = false)
    val battleDate: LocalDate = battleAt.toKoreaLocalDate()

    init {
        val battleType = EnumCodeType.findByCode<BattleType>(type)
        require(battleType != BattleType.FRIENDLY) {
            "FRIENDLY 전투는 통계 대상이 아닙니다. battleKey=$battleKey, type=$type"
        }

        val brawlerIds = teams.flatten().map { it.brawler.brawlStarsId }
        require(brawlerIds.size == brawlerIds.distinct().size) {
            "중복된 브롤러가 포함될 수 없습니다. battleKey=$battleKey, brawlerIds=$brawlerIds"
        }
    }

    /**
     * 일반적으로 player team 은 1개일 것.
     */
    fun playerTeams(): List<List<StatsBattleEntityTeamPlayer>> =
        teams.filter { team -> team.any { it.brawlStarsTag == playerBrawlStarsTag } }

    fun enemyTeams(): List<List<StatsBattleEntityTeamPlayer>> =
        teams.filter { team -> team.none { it.brawlStarsTag == playerBrawlStarsTag } }
}
