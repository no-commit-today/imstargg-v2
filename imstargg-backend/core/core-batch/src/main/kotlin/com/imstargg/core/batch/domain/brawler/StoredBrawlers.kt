package com.imstargg.core.batch.domain.brawler

/**
 * DB 에 등록된 브롤러/하위 항목의 ID 집합.
 * DB 는 API 원본(이름 등)을 보존하지 않으므로 ID 만 담는다.
 *
 * [brawlerIds]·[itemIds] 는 활성 항목, [deletedBrawlerIds]·[deletedItemIds] 는 soft-delete 된 항목이다.
 * 삭제된 항목은 API 에 남아 있어도 신규로 감지하지 않고, API 에서 사라져도 제거로 감지하지 않는다.
 */
data class StoredBrawlers(
    val brawlerIds: Set<Long>,
    val itemIds: Map<Long, Map<BrawlerItemType, Set<Long>>>,
    val deletedBrawlerIds: Set<Long> = emptySet(),
    val deletedItemIds: Map<Long, Map<BrawlerItemType, Set<Long>>> = emptyMap(),
)
