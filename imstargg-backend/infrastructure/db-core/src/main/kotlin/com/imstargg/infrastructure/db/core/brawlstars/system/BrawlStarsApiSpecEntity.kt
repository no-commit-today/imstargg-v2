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
 * content 는 core-batch 의 YamlCanonicalizer 로 맵 키를 정렬한 정규화본이다
 * (2026-07 정규화 도입 이전 행은 원문 — 도입 후 첫 실행에서 정규화본으로 이관 저장됨).
 * 직전 스냅샷과 정규화본이 다를 때만(또는 최초 수집·레거시 이관 시) 저장하는 append-only 테이블.
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
