package com.imstargg.core.batch.job.brawlstars

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerChangeDetectorTest {
    @Test
    fun `새 아이템과 삭제 아이템을 감지한다`() {
        // given
        val sut = BrawlerChangeDetector()

        // when
        val result =
            sut.detectItemChanges(
                brawlerId = 16000000,
                brawlerName = "Shelly",
                itemType = BrawlerChangeItemType.GEAR,
                apiItemIds = setOf(1, 2),
                dbItemIds = setOf(2, 3),
                toJson = { id -> "item=$id" },
            )

        // then
        assertThat(result).hasSize(2)
        assertThat(result)
            .extracting("changeType")
            .containsExactlyInAnyOrder(BrawlerChangeType.ADDED, BrawlerChangeType.REMOVED)
        assertThat(result.map { it.itemJson }).containsExactlyInAnyOrder("item=1", "brawlStarsId=3")
    }
}
