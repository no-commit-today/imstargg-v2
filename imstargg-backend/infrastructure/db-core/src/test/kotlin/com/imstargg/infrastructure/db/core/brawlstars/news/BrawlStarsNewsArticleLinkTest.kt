package com.imstargg.infrastructure.db.core.brawlstars.news

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BrawlStarsNewsArticleLinkTest {
    @Test
    fun `목록의 경로에 사이트 주소를 붙이고 끝 슬래시를 뗀다`() {
        val link = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/game-updates/release-notes-november-2026/")

        assertThat(link.linkUrl)
            .isEqualTo("https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026")
    }

    @ParameterizedTest
    @CsvSource(
        "update65-2, update65",
        "update65-5, update65",
        "brawl-cup-event-guide-2, brawl-cup-event-guide",
        "release-notes-november-2026, release-notes-november-2026",
        "balance-changes-13-2, balance-changes-13",
        "update65-2-3, update65-2",
        "patch-notes, patch-notes",
    )
    fun `마지막 구간에서 끝의 숫자 한두 자리를 한 번만 뗀다`(
        slug: String,
        key: String,
    ) {
        assertThat(BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/game-updates/$slug").articleKey)
            .isEqualTo(key)
    }
}
