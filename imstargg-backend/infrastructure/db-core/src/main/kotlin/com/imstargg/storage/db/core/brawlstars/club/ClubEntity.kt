package com.imstargg.storage.db.core.brawlstars.club

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "club",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_club__brawlstarstag", columnNames = ["brawlstars_tag"]),
    ],
)
class ClubEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    @Column(name = "badge_brawlstars_id", updatable = false, nullable = false)
    val badgeBrawlStarsId: Long,
    name: String,
    description: String?,
    type: String,
    requiredTrophies: Int,
    trophies: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    val id: Long? = null

    @Column(name = "name", length = 105, nullable = false)
    var name: String = name
        protected set

    @Column(name = "description", length = 500)
    var description: String? = description
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
