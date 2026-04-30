package com.imstargg.core.batch.job.brawlstars

data class BrawlerChange(
    val brawlerId: Long,
    val brawlerName: String,
    val changeType: BrawlerChangeType,
    val itemType: BrawlerChangeItemType,
    val itemJson: String = "",
)
