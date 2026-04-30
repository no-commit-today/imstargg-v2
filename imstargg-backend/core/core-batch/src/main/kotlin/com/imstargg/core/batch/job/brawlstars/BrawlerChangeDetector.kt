package com.imstargg.core.batch.job.brawlstars

import org.springframework.stereotype.Component

@Component
class BrawlerChangeDetector {
    fun detectItemChanges(
        brawlerId: Long,
        brawlerName: String,
        itemType: BrawlerChangeItemType,
        apiItemIds: Set<Long>,
        dbItemIds: Set<Long>,
        toJson: (Long) -> String,
    ): List<BrawlerChange> {
        val addedChanges =
            (apiItemIds - dbItemIds).map { id ->
                BrawlerChange(
                    brawlerId = brawlerId,
                    brawlerName = brawlerName,
                    changeType = BrawlerChangeType.ADDED,
                    itemType = itemType,
                    itemJson = toJson(id),
                )
            }
        val removedChanges =
            (dbItemIds - apiItemIds).map { id ->
                BrawlerChange(
                    brawlerId = brawlerId,
                    brawlerName = brawlerName,
                    changeType = BrawlerChangeType.REMOVED,
                    itemType = itemType,
                    itemJson = "brawlStarsId=$id",
                )
            }
        return addedChanges + removedChanges
    }
}
