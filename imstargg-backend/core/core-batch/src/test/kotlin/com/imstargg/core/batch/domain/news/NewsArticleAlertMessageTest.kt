package com.imstargg.core.batch.domain.news

import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class NewsArticleAlertMessageTest {
    private val path = "/en/games/brawlstars/blog/game-updates/release-notes-november-2026"
    private val url = "https://supercell.com$path"

    private fun article(
        path: String = this.path,
        title: String = "Release Notes November 2026",
        publishedAt: String = "2026-11-11T03:00:00Z",
        type: BrawlStarsNewsArticleType = BrawlStarsNewsArticleType.RELEASE_NOTE,
        content: String = "본문",
    ) = BrawlStarsNewsArticleEntity(
        link = BrawlStarsNewsArticleLink.of(path),
        title = title,
        publishedAt = Instant.parse(publishedAt),
        type = type,
        rawBody = "[]",
        content = content,
    )

    @Test
    fun `변경 알림은 바뀐 것만 적는다`() {
        val common = (1..10).map { "공통 $it" }
        val previousContent = (common + (1..4).map { "옛 $it" }).joinToString("\n")
        val content = (common + (1..19).map { "새 $it" }).joinToString("\n")
        val previous = article(publishedAt = "2026-11-03T03:00:00Z", content = previousContent)
        val current = article(publishedAt = "2026-11-11T03:00:00Z", content = content)

        val command =
            NewsArticleAlertMessage.updated(
                article = current,
                previous = previous,
                previousContent = previousContent,
                previousSeenAt = Instant.parse("2026-11-10T12:20:00Z"),
                detectedAt = Instant.parse("2026-11-11T12:20:00Z"),
            )

        assertThat(command.title).isEqualTo("[변경] [릴리스 노트] Release Notes November 2026")
        assertThat(command.content).isEqualTo(
            """
            https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026
            감지 2026-11-11 21:20 KST · 직전 판 2026-11-10 21:20 KST
            게시일 2026-11-03 → 2026-11-11
            본문 19줄 추가, 4줄 삭제
            """.trimIndent(),
        )
        assertThat(command.ex).isNull()
    }

    @Test
    fun `주소만 바뀌면 주소와 본문 동일을 적는다`() {
        val previous = article()
        val newPath = "/en/games/brawlstars/blog/game-updates/release-notes-november-2026-2"
        val current = article(path = newPath)

        val command =
            NewsArticleAlertMessage.updated(
                article = current,
                previous = previous,
                previousContent = "본문",
                previousSeenAt = Instant.parse("2026-11-10T12:20:00Z"),
                detectedAt = Instant.parse("2026-11-11T12:20:00Z"),
            )

        assertThat(command.content).isEqualTo(
            """
            https://supercell.com$newPath
            감지 2026-11-11 21:20 KST · 직전 판 2026-11-10 21:20 KST
            주소 $url → https://supercell.com$newPath
            본문 동일
            """.trimIndent(),
        )
    }

    @Test
    fun `새 글 알림은 유형과 제목과 주소와 게시일과 감지 시각을 담는다`() {
        val command =
            NewsArticleAlertMessage.newArticle(
                article = article(title = "Brawl Stars x Duolingo", type = BrawlStarsNewsArticleType.OTHER),
                detectedAt = Instant.parse("2026-11-11T12:20:00Z"),
            )

        assertThat(command.title).isEqualTo("[새 글] [기타] Brawl Stars x Duolingo")
        assertThat(command.content).isEqualTo(
            """
            $url
            게시일 2026-11-11
            감지 2026-11-11 21:20 KST
            """.trimIndent(),
        )
        assertThat(command.ex).isNull()
    }

    @Test
    fun `새 글 알림의 게시일은 KST 날짜로 적는다`() {
        val command =
            NewsArticleAlertMessage.newArticle(
                article = article(publishedAt = "2026-11-10T16:00:00Z"),
                detectedAt = Instant.parse("2026-11-10T16:30:00Z"),
            )

        assertThat(command.content).isEqualTo(
            """
            $url
            게시일 2026-11-11
            감지 2026-11-11 01:30 KST
            """.trimIndent(),
        )
    }

    @Test
    fun `첫 실행 요약은 글 수와 릴리스 노트 수와 가장 최근 릴리스 노트를 담는다`() {
        val oldNote =
            article(path = "/en/games/brawlstars/blog/game-updates/old", title = "Old Notes", publishedAt = "2026-10-01T03:00:00Z")
        val latestNote = article(title = "Release Notes November 2026", publishedAt = "2026-11-11T03:00:00Z")
        val other =
            article(
                path = "/en/games/brawlstars/blog/news/x",
                title = "Brawl Stars x Duolingo",
                publishedAt = "2026-11-12T03:00:00Z",
                type = BrawlStarsNewsArticleType.OTHER,
            )

        val command = NewsArticleAlertMessage.firstExecutionSummary(listOf(oldNote, other, latestNote))

        assertThat(command.title).isEqualTo("[첫 실행] 공식 뉴스 수집")
        assertThat(command.content).isEqualTo(
            """
            글 3건 저장, 릴리스 노트 2건
            Release Notes November 2026
            $url
            """.trimIndent(),
        )
    }

    @Test
    fun `첫 실행 요약은 릴리스 노트가 없으면 없다고 적는다`() {
        val other = article(title = "Brawl Stars x Duolingo", type = BrawlStarsNewsArticleType.OTHER)

        val command = NewsArticleAlertMessage.firstExecutionSummary(listOf(other))

        assertThat(command.content).isEqualTo("글 1건 저장, 릴리스 노트 0건\n릴리스 노트 없음")
    }
}
