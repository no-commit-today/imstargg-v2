package com.imstargg.infrastructure.db.core.brawlstars.battle

import com.imstargg.infrastructure.db.core.BaseEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
    name = "battle",
    indexes = [
        Index(name = "ix_battle__playerid_battleat", columnList = "player_id, battle_at desc"),
    ],
)
class BattleEntity(
    @Column(name = "battle_key", updatable = false, nullable = false)
    val battleKey: String,
    @Column(name = "battle_at", updatable = false, nullable = false)
    val battleAt: OffsetDateTime,
    @Embedded
    val event: BattleEntityEvent?,
    @Column(name = "mode", length = 65, updatable = false)
    val mode: String?,
    @Column(name = "type", length = 65, updatable = false)
    val type: String?,
    @Column(name = "result", length = 25, updatable = false)
    val result: String?,
    @Column(name = "duration", updatable = false)
    val duration: Int?,
    @Column(name = "star_player_brawlstars_tag", length = 45, updatable = false)
    val starPlayerBrawlStarsTag: String?,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val player: PlayerEntity,
    @Column(name = "battle_rank", updatable = false)
    val battleRank: Int?,
    @Column(name = "trophy_change", updatable = false)
    val trophyChange: Int?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teams", columnDefinition = "json", updatable = false, nullable = false)
    val teams: List<List<BattleEntityTeamPlayer>>,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "battle_id", nullable = false)
    val id: Long? = null
}
