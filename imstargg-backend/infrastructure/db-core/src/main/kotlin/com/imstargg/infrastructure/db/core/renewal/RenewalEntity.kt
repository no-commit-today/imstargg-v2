package com.imstargg.infrastructure.db.core.renewal

import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
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
import jakarta.persistence.Version
import java.time.Clock
import java.time.OffsetDateTime

@Entity
@Table(
    name = "renewal",
    indexes = [
        Index(name = "uk_renewal__brawlstarstag_type", columnList = "brawlstars_tag, type", unique = true),
    ],
)
class RenewalEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 25, updatable = false, nullable = false)
    val type: RenewalType,
    requestedAt: OffsetDateTime,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    val id: Long? = null

    @Column(name = "requested_at", nullable = false)
    var requestedAt: OffsetDateTime = requestedAt
        protected set

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

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0

    fun reRequest(requestedAt: OffsetDateTime) {
        this.requestedAt = requestedAt
        this.status = RenewalStatus.REQUESTED
        this.executedAt = null
        this.completedAt = null
    }

    fun toPending() {
        check(status == RenewalStatus.REQUESTED) {
            "Cannot transition to PENDING from $status"
        }
        status = RenewalStatus.PENDING
    }

    fun toExecuting(clock: Clock) {
        check(status == RenewalStatus.PENDING) {
            "Cannot transition to EXECUTING from $status"
        }
        executedAt = OffsetDateTime.now(clock)
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
        check(status.isProgressing()) {
            "Cannot transition to FAILED from $status"
        }
        completedAt = OffsetDateTime.now(clock)
        status = RenewalStatus.FAILED
    }
}
