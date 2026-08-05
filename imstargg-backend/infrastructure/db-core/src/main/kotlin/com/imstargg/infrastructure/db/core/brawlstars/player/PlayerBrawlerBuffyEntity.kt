package com.imstargg.infrastructure.db.core.brawlstars.player

import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    name = "player_brawler_buffy",
    indexes = [
        Index(
            name = "uk_playerbrawlerbuffy__playerbrawlerid_buffytype",
            columnList = "player_brawler_id, buffy_type",
            unique = true,
        ),
    ],
)
class PlayerBrawlerBuffyEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_brawler_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val playerBrawler: PlayerBrawlerEntity,
    @Enumerated(EnumType.STRING)
    @Column(name = "buffy_type", length = 25, updatable = false, nullable = false)
    val buffyType: BuffyType,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "playerBrawlerBuffySeq")
    @SequenceGenerator(
        name = "playerBrawlerBuffySeq",
        sequenceName = "player_brawler_buffy_seq",
        allocationSize = 100,
    )
    @Column(name = "player_brawler_buffy_id")
    val id: Long? = null
}
