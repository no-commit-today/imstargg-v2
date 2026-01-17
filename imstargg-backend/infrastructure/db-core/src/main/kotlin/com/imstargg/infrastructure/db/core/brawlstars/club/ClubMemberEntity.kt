package com.imstargg.infrastructure.db.core.brawlstars.club

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
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

@Entity
@Table(
    name = "club_member",
    indexes = [
        Index(name = "uk_clubmember__clubid_brawlstarstag", columnList = "club_id, brawlstars_tag", unique = true),
    ],
)
class ClubMemberEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val club: ClubEntity,
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    name: String,
    nameColor: String?,
    role: String,
    trophies: Int,
    iconBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_id")
    val id: Long? = null

    @Column(name = "name", length = 45, nullable = false)
    var name: String = name
        protected set

    @Column(name = "name_color", length = 45)
    var nameColor: String? = nameColor
        protected set

    @Column(name = "role", length = 25, nullable = false)
    var role: String = role
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Column(name = "icon_brawlstars_id", nullable = false)
    var iconBrawlStarsId: Long = iconBrawlStarsId
        protected set
}
