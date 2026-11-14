package com.imstargg.core.admin.response.brawlstars

import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchEntity
import java.time.LocalDate

data class BrawlStarsPatchResponse(
    val id: Long,
    val patchDate: LocalDate,
    val memo: String?,
) {
    companion object {
        fun of(entity: BrawlStarsPatchEntity): BrawlStarsPatchResponse =
            BrawlStarsPatchResponse(
                id = requireNotNull(entity.id),
                patchDate = entity.patchDate,
                memo = entity.memo,
            )
    }
}
