package com.imstargg.core.batch.domain.brawler

/**
 * API 스냅샷과 DB 상태를 비교해 브롤러/하위 항목의 생성·삭제를 감지한다.
 *
 * 신규/삭제 브롤러는 하위 항목 변경을 따로 방출하지 않는다 —
 * 신규는 모든 항목이 새 항목이라 자명하고(알림에 브롤러 전체가 담김), 삭제는 브롤러 자체가 사라지므로 노이즈다.
 */
class BrawlerChangeDetector {
    fun detect(
        apiBrawlers: List<BrawlerSnapshot>,
        stored: StoredBrawlers,
    ): List<BrawlerChange> {
        val apiIds = apiBrawlers.map { it.id }.toSet()

        val addedBrawlers =
            apiBrawlers
                .filter { it.id !in stored.brawlerIds }
                .map { BrawlerChange.BrawlerAdded(brawlerId = it.id, brawlerName = it.name) }
        val removedBrawlers =
            (stored.brawlerIds - apiIds)
                .map { BrawlerChange.BrawlerRemoved(brawlerId = it) }
        val itemChanges =
            apiBrawlers
                .filter { it.id in stored.brawlerIds }
                .flatMap { detectItemChanges(it, stored.itemIds[it.id].orEmpty()) }

        return addedBrawlers + removedBrawlers + itemChanges
    }

    private fun detectItemChanges(
        brawler: BrawlerSnapshot,
        storedItemIds: Map<BrawlerItemType, Set<Long>>,
    ): List<BrawlerChange> =
        BrawlerItemType.entries.flatMap { itemType ->
            val apiItems = brawler.items[itemType].orEmpty()
            val apiItemIds = apiItems.map { it.id }.toSet()
            val dbItemIds = storedItemIds[itemType].orEmpty()

            val addedItems =
                apiItems
                    .filter { it.id !in dbItemIds }
                    .map {
                        BrawlerChange.ItemAdded(
                            brawlerId = brawler.id,
                            brawlerName = brawler.name,
                            itemType = itemType,
                            itemId = it.id,
                            itemName = it.name,
                        )
                    }
            val removedItems =
                (dbItemIds - apiItemIds)
                    .map {
                        BrawlerChange.ItemRemoved(
                            brawlerId = brawler.id,
                            brawlerName = brawler.name,
                            itemType = itemType,
                            itemId = it,
                        )
                    }
            addedItems + removedItems
        }
}
