package com.imstargg.infrastructure.db.core.statistics.brawler

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 소유율 통계의 분자(소유 플레이어 수)와 분모(해당 브롤러·트로피 구간의 전체 플레이어 수).
 *
 * [totalCount] 는 [ownerCount] 에서 파생되는 값이 아니라 별도로 집계된 분모이므로
 * `ResultCountEmbedded` 와 달리 팩토리 없이 생성자로 직접 받는다.
 *
 * `kotlin("plugin.jpa")`(noArg) 로 Hibernate 는 무인자 생성자 + 필드 주입으로 하이드레이션하므로
 * 아래 불변식은 쓰기 경로에서만 검사된다. 기존 데이터가 불변식을 어겨도 조회는 실패하지 않는다.
 */
@Embeddable
data class OwnershipCountEmbedded(
    @Column(name = "owner_count", updatable = false, nullable = false)
    val ownerCount: Long,
    @Column(name = "total_count", updatable = false, nullable = false)
    val totalCount: Long,
) {
    init {
        require(ownerCount >= 0) { "ownerCount($ownerCount)는 0 이상이어야 합니다." }
        require(ownerCount <= totalCount) {
            "ownerCount($ownerCount)는 totalCount($totalCount)를 넘을 수 없습니다."
        }
    }
}
