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
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.OffsetDateTime

@Entity
@Table(
    name = "renewal",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_renewal__brawlstarstag_type",
            columnNames = ["brawlstars_tag", "type"],
        ),
    ],
)
class RenewalEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 25, updatable = false, nullable = false)
    val type: RenewalType,
    status: RenewalStatus,
    renewedAt: OffsetDateTime,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    val id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    var status: RenewalStatus = status
        protected set

    @Column(name = "renewed_at", nullable = false)
    var renewedAt: OffsetDateTime = renewedAt
        protected set

    @Version
    val version: Int = 0
}
