package com.imstargg.infrastructure.db.core.brawlstars.system

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 브롤스타즈 API 스펙(`GET /v1`, swagger 2.0 YAML) 스냅샷.
 *
 * 직전 스냅샷과 내용이 다를 때만(또는 최초 수집 시) 저장하는 append-only 테이블.
 * 행이 연 수 건 수준으로만 늘어나므로 인덱스를 두지 않는다 (PK 만 사용).
 */
@Entity
@Table(name = "brawlstars_api_spec")
class BrawlStarsApiSpecEntity(
    @Column(name = "content", updatable = false, nullable = false, columnDefinition = "text")
    val content: String,
    @Column(name = "collected_at", updatable = false, nullable = false)
    val collectedAt: Instant,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_api_spec_id")
    val id: Long? = null
}
