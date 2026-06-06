package com.imstargg.core.admin.response.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import java.time.Instant

data class BrawlStarsApiSpecDetailResponse(
    val id: Long,
    val collectedAt: Instant,
    val content: String,
    val previousId: Long?,
    val previousCollectedAt: Instant?,
    val previousContent: String?,
) {
    companion object {
        fun of(
            entity: BrawlStarsApiSpecEntity,
            previous: BrawlStarsApiSpecEntity?,
        ): BrawlStarsApiSpecDetailResponse =
            BrawlStarsApiSpecDetailResponse(
                id = requireNotNull(entity.id),
                collectedAt = entity.collectedAt,
                content = entity.content,
                previousId = previous?.id,
                previousCollectedAt = previous?.collectedAt,
                previousContent = previous?.content,
            )
    }
}
