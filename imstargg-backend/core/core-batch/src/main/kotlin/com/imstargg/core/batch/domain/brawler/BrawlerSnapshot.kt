package com.imstargg.core.batch.domain.brawler

/**
 * 브롤스타즈 API 응답을 평탄화한 브롤러 스냅샷.
 * domain 이 client 모듈의 응답 타입에 의존하지 않도록 호출부에서 변환해 전달한다.
 */
data class BrawlerSnapshot(
    val id: Long,
    val name: String,
    val items: Map<BrawlerItemType, Set<BrawlerItemRef>>,
)
