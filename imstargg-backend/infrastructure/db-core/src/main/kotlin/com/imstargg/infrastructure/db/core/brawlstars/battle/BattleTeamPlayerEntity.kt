package com.imstargg.infrastructure.db.core.brawlstars.battle

import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(
    name = "battle_team_player",
    indexes = [
        Index(name = "ix_battleteamplayer__battleid", columnList = "battle_id"),
    ],
)
class BattleTeamPlayerEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val battle: BattleEntity,
    @Column(name = "team_idx", updatable = false, nullable = false)
    val teamIdx: Int,
    @Column(name = "player_idx", updatable = false, nullable = false)
    val playerIdx: Int,
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    @Column(name = "name", length = 105, updatable = false, nullable = false)
    val name: String,
    @Embedded
    val brawler: BattleTeamPlayerEntityBrawler,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "battleTeamPlayerSeq")
    @SequenceGenerator(
        name = "battleTeamPlayerSeq",
        sequenceName = "battle_team_player_seq",
        allocationSize = 100,
    )
    @Column(name = "battle_team_player_id", nullable = false)
    val id: Long? = null
}

fun List<BattleTeamPlayerEntity>.toTeams(): List<List<BattleTeamPlayerEntity>> =
    groupBy { it.teamIdx }
        .toSortedMap()
        .values
        .map { team -> team.sortedBy { it.playerIdx } }
