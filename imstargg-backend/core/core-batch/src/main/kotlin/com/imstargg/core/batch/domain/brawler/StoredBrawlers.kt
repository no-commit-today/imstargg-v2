package com.imstargg.core.batch.domain.brawler

/**
 * DB 에 등록된 브롤러/하위 항목의 ID 집합.
 * DB 는 API 원본(이름 등)을 보존하지 않으므로 ID 만 담는다.
 */
data class StoredBrawlers(
    val brawlerIds: Set<Long>,
    val itemIds: Map<Long, Map<BrawlerItemType, Set<Long>>>,
)
