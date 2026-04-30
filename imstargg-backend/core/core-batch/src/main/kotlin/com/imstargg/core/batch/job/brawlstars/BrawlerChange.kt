package com.imstargg.core.batch.job.brawlstars

enum class BrawlerChangeType {
    ADDED,
    REMOVED,
}

enum class BrawlerChangeItemType {
    STAR_POWER,
    GADGET,
    HYPER_CHARGE,
    GEAR,
}

data class BrawlerChange(
    val brawlerId: Long,
    val brawlerName: String,
    val changeType: BrawlerChangeType,
    val itemType: BrawlerChangeItemType,
    val itemJson: String = "",
)
