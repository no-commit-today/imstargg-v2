package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType

class BrawlStarsImageEntityFixture {
    private var type: BrawlStarsImageType = BrawlStarsImageType.BRAWLER_PORTRAIT
    private var brawlStarsId: Long = 16000000L
    private var storedName: String = "brawler/16000000/portrait"
    private var url: String = "http://localhost:4566/imstargg-images-local/brawler/16000000/portrait"

    companion object {
        fun aBrawlStarsImageEntity() = BrawlStarsImageEntityFixture()
    }

    fun withType(value: BrawlStarsImageType) = apply { type = value }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withStoredName(value: String) = apply { storedName = value }

    fun withUrl(value: String) = apply { url = value }

    fun build() =
        BrawlStarsImageEntity(
            type = type,
            brawlStarsId = brawlStarsId,
            storedName = storedName,
            url = url,
        )
}
