package com.imstargg.infrastructure.db.core

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.Clock
import java.time.Instant

/**
 * 게임에서 제거됐지만 행은 남겨야 하는 브롤스타즈 자원(브롤러·기어·가젯 등)의 soft-delete 베이스.
 *
 * 행을 남기는 이유: 브롤스타즈 API 는 게임에서 제거된 항목을 계속 응답하는 경우가 있어(예: 브롤러별 기어),
 * 행을 지우면 동기화 배치가 같은 항목을 매번 "신규"로 감지한다. 삭제 표시만 남겨 "이미 알고 있는 항목"으로 취급한다.
 *
 * `@SQLRestriction` 으로 전역 필터링하지 않는다 — 동기화 배치·관리자는 삭제된 행도 읽어야 하므로
 * 조회 시점에 `...DeletedAtIsNull` 쿼리 메서드로 명시적으로 거른다.
 */
@MappedSuperclass
abstract class BaseSoftDeleteEntity : BaseTimeEntity() {
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        protected set

    val isDeleted: Boolean
        get() = deletedAt != null

    fun delete(clock: Clock) {
        this.deletedAt = Instant.now(clock)
    }

    fun restore() {
        this.deletedAt = null
    }
}
