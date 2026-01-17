package com.imstargg.infrastructure.db.core.brawlstars.club

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
) : BaseEntity() {
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
}
