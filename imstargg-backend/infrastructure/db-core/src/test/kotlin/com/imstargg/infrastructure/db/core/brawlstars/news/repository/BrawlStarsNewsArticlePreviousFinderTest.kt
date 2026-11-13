package com.imstargg.infrastructure.db.core.brawlstars.news.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class BrawlStarsNewsArticlePreviousFinderTest(
    private val repository: BrawlStarsNewsArticleJpaRepository,
) : DbCoreContextTest() {
    private fun link(slug: String) = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/game-updates/$slug")

    private fun save(slug: String) =
        repository.saveAndFlush(
            BrawlStarsNewsArticleEntity(
                link = link(slug),
                title = "제목",
                publishedAt = Instant.parse("2026-11-01T00:00:00Z"),
                type = BrawlStarsNewsArticleType.OTHER,
                rawBody = "raw",
                content = "content",
            ),
        )

    @Test
    fun `같은 주소의 행이 있으면 주소 키가 같은 더 최근 행보다 그것을 고른다`() {
        val first = save("update65-2")
        save("update65-3")

        val previous = repository.findPrevious(link("update65-2"))

        assertThat(previous?.id).isEqualTo(first.id)
    }

    @Test
    fun `같은 주소의 행이 없으면 같은 주소 키의 가장 최근 행을 고른다`() {
        val first = save("update65-2")

        val previous = repository.findPrevious(link("update65-3"))

        assertThat(previous?.id).isEqualTo(first.id)
    }

    @Test
    fun `beforeId 보다 앞선 행만 본다`() {
        val first = save("update65-2")
        val second = save("update65-2")

        assertThat(repository.findPrevious(link("update65-2"), beforeId = second.id!!)?.id).isEqualTo(first.id)
        assertThat(repository.findPrevious(link("update65-2"), beforeId = first.id!!)).isNull()
    }

    @Test
    fun `아무 행도 없으면 null 이다`() {
        assertThat(repository.findPrevious(link("update65-2"))).isNull()
    }
}
