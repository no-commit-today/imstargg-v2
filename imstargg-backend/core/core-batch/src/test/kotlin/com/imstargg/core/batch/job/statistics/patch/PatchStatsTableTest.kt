package com.imstargg.core.batch.job.statistics.patch

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PatchStatsTableTest {
    @Test
    fun `키 컬럼에 tier_range 가 없으면 만들 수 없다`() {
        assertThatThrownBy {
            PatchStatsTable(
                name = "brawler_battle_result_stats",
                keyColumns = listOf("event_brawlstars_id", "brawler_brawlstars_id"),
                countColumns = listOf("victory_count", "total_count"),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("tier_range")
    }

    @Test
    fun `값 컬럼에 total_count 가 없으면 만들 수 없다`() {
        assertThatThrownBy {
            PatchStatsTable(
                name = "brawler_battle_result_stats",
                keyColumns = listOf("tier_range", "event_brawlstars_id", "brawler_brawlstars_id"),
                countColumns = listOf("victory_count", "defeat_count"),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("total_count")
    }
}
