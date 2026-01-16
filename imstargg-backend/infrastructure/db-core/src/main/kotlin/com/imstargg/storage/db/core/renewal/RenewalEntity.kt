package com.imstargg.storage.db.core.renewal

import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.OffsetDateTime

@Entity
@Table(
    name = "renewal",
    indexes = [
        Index(name = "ix_renewal__brawlstarstag_type_requestedat", columnList = "brawlstars_tag, type, requested_at desc"),
    ],
)
class RenewalEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 25, updatable = false, nullable = false)
    val type: RenewalType,
    @Column(name = "requested_at", nullable = false)
    val requestedAt: OffsetDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    var status: RenewalStatus = RenewalStatus.PENDING
        protected set

    @Column(name = "completed_at", nullable = true)
    var completedAt: OffsetDateTime? = null
        protected set

    @Version
    val version: Int = 0
}
