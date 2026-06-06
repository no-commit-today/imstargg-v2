package com.imstargg.infrastructure.db.core.brawlstars

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "brawlstars_api_maintenance",
    indexes = [
        Index(name = "ix_brawlstars_api_maintenance__end_time", columnList = "end_time"),
    ],
)
class BrawlStarsApiMaintenanceEntity(
    @Column(name = "start_time", updatable = false, nullable = false)
    val startTime: Instant,
    endTime: Instant,
) : BaseTimeEntity() {
    init {
        require(!startTime.isAfter(endTime)) { "startTime must be before or equal to endTime" }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_api_maintenance_id")
    val id: Long? = null

    @Column(name = "end_time", nullable = false)
    var endTime: Instant = endTime
        protected set

    fun isActive(now: Instant): Boolean = !now.isBefore(startTime) && !now.isAfter(endTime)
}
