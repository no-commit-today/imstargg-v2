package com.imstargg.storage.db.core.brawlstars.battle

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
    name = "battle",
    indexes = [
        Index(name = "ix_battle__battletime", columnList = "battle_time desc"),
        Index(name = "ix_battle__playerid_battletime", columnList = "player_id, battle_time desc"),
        Index(name = "ix_event_type_battletime", columnList = "event_brawlstars_id, type, battle_time desc"),
    ],
)
class BattleEntity(
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,

    @Column(name = "battle_time", updatable = false, nullable = false)
    val battleTime: OffsetDateTime,

    @Embedded
    val event: BattleEntityEvent,

    @Column(name = "mode", length = 105, updatable = false, nullable = false)
    val mode: String,

    @Column(name = "type", length = 105, updatable = false)
    val type: String?,

    @Column(name = "result", length = 25, updatable = false)
    val result: String?,

    @Column(name = "duration", updatable = false)
    val duration: Int?,

    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,

    @Embedded
    val player: BattleEntityPlayer,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<BattleEntityTeamPlayer>>,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_id", nullable = false)
    val id: Long? = null
}
