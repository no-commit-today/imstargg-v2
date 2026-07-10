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
import java.time.Instant

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
    requestedAt: Instant,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    val id: Long? = null

    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = requestedAt
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    var status: RenewalStatus = RenewalStatus.REQUESTED
        protected set

    @Column(name = "executed_at", nullable = true)
    var executedAt: Instant? = null
        protected set

    @Column(name = "completed_at", nullable = true)
    var completedAt: Instant? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0

    /**
     * 도메인이 계산한 요청-측 상태를 그대로 반영한다 (요청/재요청). 상태 전이 규칙은 도메인이 소유하며
     * 이 메서드는 필드 동기화만 한다. 모듈 경계상(db-core ↛ core-api) 도메인 객체 대신 필드를 받는다.
     */
    fun update(
        status: RenewalStatus,
        requestedAt: Instant,
        executedAt: Instant?,
        completedAt: Instant?,
    ) {
        this.status = status
        this.requestedAt = requestedAt
        this.executedAt = executedAt
        this.completedAt = completedAt
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
        executedAt = clock.instant()
        status = RenewalStatus.EXECUTING
    }

    fun toCompleted(clock: Clock) {
        check(status == RenewalStatus.EXECUTING) {
            "Cannot transition to COMPLETED from $status"
        }
        completedAt = clock.instant()
        status = RenewalStatus.COMPLETED
    }

    fun toNotFound(clock: Clock) {
        check(status == RenewalStatus.EXECUTING) {
            "Cannot transition to NOT_FOUND from $status"
        }
        completedAt = clock.instant()
        status = RenewalStatus.NOT_FOUND
    }

    fun toFailed(clock: Clock) {
        check(status.isProgressing()) {
            "Cannot transition to FAILED from $status"
        }
        completedAt = clock.instant()
        status = RenewalStatus.FAILED
    }
}
