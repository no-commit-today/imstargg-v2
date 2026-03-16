package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.core.enums.BrawlStarsEntityStatus
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "brawler",
    indexes = [
        Index(name = "uk_brawler__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class BrawlerEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val rarity: BrawlerRarity,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val role: BrawlerRole,
    status: BrawlStarsEntityStatus = BrawlStarsEntityStatus.CREATED,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "varchar(25)", nullable = false)
    var status: BrawlStarsEntityStatus = status
        protected set

    fun nameMessageCode(): MessageCode = MessageCode.of("brawler", brawlStarsId, "name")

    fun portraitImageCode(): BrawlStarsImageCode = BrawlStarsImageCode.of("brawler", brawlStarsId, "portrait")
}
