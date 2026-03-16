package com.imstargg.core.enums

enum class BrawlStarsEntityStatus {
    CREATED,
    ACTIVE,
    RETIRED,
    ;

    companion object {
        fun initializedStatuses(): List<BrawlStarsEntityStatus> = listOf(ACTIVE, RETIRED)
    }
}
