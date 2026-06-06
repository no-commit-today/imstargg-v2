package com.imstargg.infrastructure.db.core.brawlstars.player

import com.imstargg.core.enums.ClubStatus
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

@Entity
@Table(
    name = "club",
    indexes = [
        Index(name = "uk_club__brawlstarstag", columnList = "brawlstars_tag", unique = true),
        // 아래 두 인덱스는 schema.sql 에서 varchar_pattern_ops(LIKE 패턴 검색용) 연산자 클래스를 사용 —
        // JPA @Index 로는 연산자 클래스를 표현할 수 없어 columnList 는 컬럼명만 반영 (실제 DDL 은 schema.sql 참조)
        Index(name = "ix_club__name", columnList = "name"),
        Index(name = "ix_club__brawlstars_tag", columnList = "brawlstars_tag"),
    ],
)
class ClubEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    badgeBrawlStarsId: Long,
    name: String,
    description: String,
    type: String,
    requiredTrophies: Int,
    trophies: Int,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    val id: Long? = null

    @Column(name = "badge_brawlstars_id", nullable = false)
    var badgeBrawlStarsId: Long = badgeBrawlStarsId
        protected set

    @Column(name = "name", length = 105, nullable = false)
    var name: String = name
        protected set

    @Column(name = "description", length = 500, nullable = false)
    var description: String = description
        protected set

    @Column(name = "type", length = 45, nullable = false)
    var type: String = type
        protected set

    @Column(name = "required_trophies", nullable = false)
    var requiredTrophies: Int = requiredTrophies
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    var status: ClubStatus = ClubStatus.ACTIVE
        protected set

    fun toDeleted() {
        status = ClubStatus.DELETED
    }

    fun update(
        badgeBrawlStarsId: Long,
        name: String,
        description: String,
        type: String,
        requiredTrophies: Int,
        trophies: Int,
    ) {
        this.badgeBrawlStarsId = badgeBrawlStarsId
        this.name = name
        this.description = description
        this.type = type
        this.requiredTrophies = requiredTrophies
        this.trophies = trophies
    }
}
