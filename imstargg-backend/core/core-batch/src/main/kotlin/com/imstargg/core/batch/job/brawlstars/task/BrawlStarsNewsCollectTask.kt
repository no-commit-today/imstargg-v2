package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.core.batch.domain.news.NewsArticleAlertMessage
import com.imstargg.core.batch.domain.news.NewsArticleContentExtractor
import com.imstargg.core.batch.domain.news.NewsArticleTypeClassifier
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.blog.BrawlStarsBlogArticleSummary
import com.imstargg.core.batch.support.blog.BrawlStarsBlogClient
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.BrawlStarsNewsArticleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.findPrevious
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

private val log = KotlinLogging.logger {}

/**
 * 공식 블로그의 글 목록(1~3쪽)과 본문을 읽어 판 단위로 저장하고 Discord 로 알린다.
 *
 * - 글 테이블이 비어 있으면 첫 실행이다. 모든 글을 `saveAll` 한 번으로 저장하고 요약 알림 1건만 보낸다.
 *   중간에 실패하면 한 건도 남지 않는다.
 * - 아니면 게시일이 늦은 글부터 하나씩 직전 판과 견준다. 주소·제목·본문 글자 중 하나라도 다르면 새 판을 저장하고 알린다.
 *   게시일은 비교하지 않는다.
 * - 직전 판의 `rawBody` 를 지금 규칙으로 다시 뽑은 글자가 저장된 `content` 와 다르면(추출 규칙이 바뀐 것)
 *   같은 주소 키의 모든 판의 `content` 만 다시 만들고 알리지 않는다.
 *
 * 이 클래스에는 트랜잭션을 두지 않는다. 리포지토리 호출 하나가 트랜잭션 하나이고,
 * 예외는 잡지 않으므로 실패한 실행의 앞쪽 저장은 남고 다음 실행이 남은 글만 이어서 처리한다.
 * 새 판은 저장한 뒤에 알리므로 알림이 실패해도 저장은 남는다.
 */
@Component
class BrawlStarsNewsCollectTask(
    private val clock: Clock,
    private val blogClient: BrawlStarsBlogClient,
    private val repository: BrawlStarsNewsArticleJpaRepository,
    private val alertManager: AlertManager,
    @Value("\${app.brawlstars-news.request-interval-min-millis:100}")
    private val requestIntervalMinMillis: Long,
    @Value("\${app.brawlstars-news.request-interval-max-millis:1000}")
    private val requestIntervalMaxMillis: Long,
) {
    init {
        require(requestIntervalMinMillis >= 0) { "요청 간격의 최소값은 0 이상이어야 한다 - $requestIntervalMinMillis" }
        require(requestIntervalMinMillis <= requestIntervalMaxMillis) {
            "요청 간격의 최소값이 최대값보다 클 수 없다 - min: $requestIntervalMinMillis, max: $requestIntervalMaxMillis"
        }
    }

    fun collect() {
        log.info { "[$STEP_NAME] 시작" }
        val detectedAt = clock.instant()
        var requested = false
        val pause = {
            // 실행의 첫 요청 전에는 쉬지 않는다. 상태를 지역에 두어 실행마다 다시 시작한다.
            if (requested) sleepRandomInterval()
            requested = true
        }

        val summaries =
            (1..LIST_PAGE_COUNT)
                .flatMap {
                    pause()
                    blogClient.getArticleSummaries(it)
                }.distinctBy { it.link.linkUrl }

        if (repository.count() == 0L) {
            collectFirstRun(summaries, pause)
        } else {
            summaries
                .sortedByDescending { it.publishedAt }
                .forEach { collectArticle(it, detectedAt, pause) }
        }
        log.info { "[$STEP_NAME] 완료" }
    }

    private fun collectFirstRun(
        summaries: List<BrawlStarsBlogArticleSummary>,
        pause: () -> Unit,
    ) {
        val articles =
            summaries
                .sortedBy { it.publishedAt }
                .map { summary ->
                    pause()
                    val rawBody = blogClient.getRawBody(summary.link)
                    newEntity(
                        summary = summary,
                        type = NewsArticleTypeClassifier.classify(summary.title),
                        rawBody = rawBody,
                        content = NewsArticleContentExtractor.extract(rawBody),
                    )
                }
        val saved = repository.saveAll(articles)
        log.info { "[$STEP_NAME] 첫 실행 - 글 ${saved.size}건 저장" }
        alertManager.alert(NewsArticleAlertMessage.firstRunSummary(saved))
    }

    private fun collectArticle(
        summary: BrawlStarsBlogArticleSummary,
        detectedAt: Instant,
        pause: () -> Unit,
    ) {
        pause()
        val rawBody = blogClient.getRawBody(summary.link)
        val content = NewsArticleContentExtractor.extract(rawBody)
        val previous = repository.findPrevious(summary.link)
        val previousContent = previous?.let { NewsArticleContentExtractor.extract(it.rawBody) }

        if (previous != null && previousContent != previous.content) {
            realignContents(previous.link.articleKey)
        }

        if (previous != null &&
            previous.link.linkUrl == summary.link.linkUrl &&
            previous.title == summary.title &&
            previousContent == content
        ) {
            return
        }

        val saved =
            repository.save(
                newEntity(
                    summary = summary,
                    type = previous?.type ?: NewsArticleTypeClassifier.classify(summary.title),
                    rawBody = rawBody,
                    content = content,
                ),
            )
        log.info { "[$STEP_NAME] 새 판 저장 - ${summary.link.linkUrl}" }
        val alert =
            if (previous == null || previousContent == null) {
                NewsArticleAlertMessage.newArticle(article = saved, detectedAt = detectedAt)
            } else {
                NewsArticleAlertMessage.changed(
                    article = saved,
                    previous = previous,
                    previousContent = previousContent,
                    previousSeenAt = checkNotNull(previous.createdAt),
                    detectedAt = detectedAt,
                )
            }
        alertManager.alert(alert)
    }

    /** 추출 규칙이 바뀐 경우, 같은 글의 모든 판의 `content` 를 각 판의 `rawBody` 로 다시 만든다. */
    private fun realignContents(articleKey: String) {
        val articles = repository.findAllByLinkArticleKeyOrderByIdAsc(articleKey)
        articles.forEach { it.changeContent(NewsArticleContentExtractor.extract(it.rawBody)) }
        repository.saveAll(articles)
        log.info { "[$STEP_NAME] 추출 규칙 변경 - $articleKey 의 ${articles.size}건 본문 글자를 다시 만든다" }
    }

    private fun newEntity(
        summary: BrawlStarsBlogArticleSummary,
        type: BrawlStarsNewsArticleType,
        rawBody: String,
        content: String,
    ) = BrawlStarsNewsArticleEntity(
        link = summary.link,
        title = summary.title,
        publishedAt = summary.publishedAt,
        type = type,
        rawBody = rawBody,
        content = content,
    )

    /** 최소·최대(양끝 포함) 사이의 무작위 시간만큼 쉰다. 0 이면 쉬지 않는다. */
    private fun sleepRandomInterval() {
        val millis = ThreadLocalRandom.current().nextLong(requestIntervalMinMillis, requestIntervalMaxMillis + 1)
        if (millis > 0) {
            Thread.sleep(millis)
        }
    }

    companion object {
        private const val STEP_NAME = "brawlStarsNewsCollectStep"
        private const val LIST_PAGE_COUNT = 3
    }
}
