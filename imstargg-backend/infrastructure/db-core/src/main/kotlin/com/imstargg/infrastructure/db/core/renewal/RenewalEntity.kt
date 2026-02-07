package com.imstargg.infrastructure.db.core.renewal

import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Clock
import java.time.OffsetDateTime

@Entity
@Table(
    name = "renewal",
    indexes = [
        Index(name = "ix_renewal__brawlstarstag_requestedat", columnList = "brawlstars_tag, requested_at desc"),
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
    var status: RenewalStatus = RenewalStatus.REQUESTED
        protected set

    @Column(name = "executed_at", nullable = true)
    var executedAt: OffsetDateTime? = null
        protected set

    @Column(name = "completed_at", nullable = true)
    var completedAt: OffsetDateTime? = null
        protected set

    fun toPending() {
        check(status == RenewalStatus.REQUESTED) {
            "Cannot transition to PENDING from $status"
        }
        status = RenewalStatus.PENDING
    }

    fun toExecuting(clock: Clock) {
        check(status == RenewalStatus.REQUESTED || status == RenewalStatus.PENDING || status == RenewalStatus.EXECUTING) {
            "Cannot transition to EXECUTING from $status"
        }
        if (executedAt == null) {
            executedAt = OffsetDateTime.now(clock)
        }
        status = RenewalStatus.EXECUTING
    }

    fun toCompleted(clock: Clock) {
        check(status == RenewalStatus.EXECUTING) {
            "Cannot transition to COMPLETED from $status"
        }
        completedAt = OffsetDateTime.now(clock)
        status = RenewalStatus.COMPLETED
    }

    fun toNotFound(clock: Clock) {
        check(status == RenewalStatus.EXECUTING) {
            "Cannot transition to NOT_FOUND from $status"
        }
        completedAt = OffsetDateTime.now(clock)
        status = RenewalStatus.NOT_FOUND
    }

    fun toFailed(clock: Clock) {
        check(status == RenewalStatus.EXECUTING) {
            "Cannot transition to FAILED from $status"
        }
        completedAt = OffsetDateTime.now(clock)
        status = RenewalStatus.FAILED
    }
}
