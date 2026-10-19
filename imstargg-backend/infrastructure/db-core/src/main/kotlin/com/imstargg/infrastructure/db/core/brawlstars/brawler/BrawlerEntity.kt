package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.infrastructure.db.core.BaseSoftDeleteEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 삭제(soft-delete)된 브롤러도 core-api 조회에서 거르지 않는다 —
 * 과거 전투·플레이어 데이터가 브롤러를 ID 로 참조해 이름·이미지를 표시해야 하기 때문이다.
 * 삭제 표시는 `brawlerSyncJob` 이 API 에 남아 있는 제거된 브롤러를 신규로 오인하지 않게 하는 용도다.
 */
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
) : BaseSoftDeleteEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_id")
    val id: Long? = null
}
