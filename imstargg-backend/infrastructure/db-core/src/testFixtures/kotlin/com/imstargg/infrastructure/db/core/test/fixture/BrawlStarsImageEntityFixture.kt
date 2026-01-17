package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity

class BrawlStarsImageEntityFixture {
    private var code: BrawlStarsImageCode = BrawlStarsImageCode("brawler-shelly")
    private var storedName: String = "brawler-shelly.png"

    companion object {
        fun aBrawlStarsImageEntity() = BrawlStarsImageEntityFixture()
    }

    fun withCode(value: BrawlStarsImageCode) = apply { code = value }

    fun withStoredName(value: String) = apply { storedName = value }

    fun build() =
        BrawlStarsImageEntity(
            code = code,
            storedName = storedName,
        )
}
