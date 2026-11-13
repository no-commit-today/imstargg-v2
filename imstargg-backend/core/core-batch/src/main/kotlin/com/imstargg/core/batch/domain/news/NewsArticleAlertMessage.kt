package com.imstargg.core.batch.domain.news

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 공식 뉴스 수집이 Discord 로 보내는 알림 문구.
 *
 * 시각은 `yyyy-MM-dd HH:mm KST`, 게시일은 KST 날짜 `yyyy-MM-dd` 로 적는다. 본문은 싣지 않는다.
 */
object NewsArticleAlertMessage {
    private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'KST'").withZone(KOREA_ZONE)
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(KOREA_ZONE)

    fun newArticle(
        article: BrawlStarsNewsArticleEntity,
        detectedAt: Instant,
    ): AlertCommand =
        AlertCommand(
            title = "[새 글] [${typeLabel(article.type)}] ${article.title}",
            content =
                listOf(
                    article.link.linkUrl,
                    "게시일 ${date(article.publishedAt)}",
                    "감지 ${dateTime(detectedAt)}",
                ).joinToString("\n"),
            ex = null,
        )

    fun changed(
        article: BrawlStarsNewsArticleEntity,
        previous: BrawlStarsNewsArticleEntity,
        previousContent: String,
        previousSeenAt: Instant,
        detectedAt: Instant,
    ): AlertCommand {
        val lines =
            mutableListOf(
                article.link.linkUrl,
                "감지 ${dateTime(detectedAt)} · 직전 판 ${dateTime(previousSeenAt)}",
            )
        if (previous.link.linkUrl != article.link.linkUrl) {
            lines += "주소 ${previous.link.linkUrl} → ${article.link.linkUrl}"
        }
        if (previous.title != article.title) {
            lines += "제목 ${previous.title} → ${article.title}"
        }
        if (date(previous.publishedAt) != date(article.publishedAt)) {
            lines += "게시일 ${date(previous.publishedAt)} → ${date(article.publishedAt)}"
        }
        val diff = NewsArticleContentDiff.of(previousContent, article.content)
        lines +=
            if (diff.addedLineCount == 0 && diff.removedLineCount == 0) {
                "본문 동일"
            } else {
                "본문 ${diff.addedLineCount}줄 추가, ${diff.removedLineCount}줄 삭제"
            }
        return AlertCommand(
            title = "[변경] [${typeLabel(article.type)}] ${article.title}",
            content = lines.joinToString("\n"),
            ex = null,
        )
    }

    fun firstRunSummary(articles: List<BrawlStarsNewsArticleEntity>): AlertCommand {
        val releaseNotes = articles.filter { it.type == BrawlStarsNewsArticleType.RELEASE_NOTE }
        val lines = mutableListOf("글 ${articles.size}건 저장, 릴리스 노트 ${releaseNotes.size}건")
        val latest = releaseNotes.maxByOrNull { it.publishedAt }
        if (latest == null) {
            lines += "릴리스 노트 없음"
        } else {
            lines += latest.title
            lines += latest.link.linkUrl
        }
        return AlertCommand(
            title = "[첫 실행] 공식 뉴스 수집",
            content = lines.joinToString("\n"),
            ex = null,
        )
    }

    private fun typeLabel(type: BrawlStarsNewsArticleType): String =
        when (type) {
            BrawlStarsNewsArticleType.RELEASE_NOTE -> "릴리스 노트"
            BrawlStarsNewsArticleType.OTHER -> "기타"
        }

    private fun dateTime(instant: Instant): String = DATE_TIME_FORMAT.format(instant)

    private fun date(instant: Instant): String = DATE_FORMAT.format(instant)
}
