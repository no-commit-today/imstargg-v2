package com.imstargg.core.admin.response.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import java.time.Instant

data class BrawlStarsApiSpecListItemResponse(
    val id: Long,
    val collectedAt: Instant,
    val sizeBytes: Int,
    val lineCount: Int,
) {
    companion object {
        fun of(entity: BrawlStarsApiSpecEntity): BrawlStarsApiSpecListItemResponse =
            BrawlStarsApiSpecListItemResponse(
                id = requireNotNull(entity.id),
                collectedAt = entity.collectedAt,
                sizeBytes = entity.content.toByteArray().size,
                lineCount = entity.content.lines().size,
            )
    }
}
