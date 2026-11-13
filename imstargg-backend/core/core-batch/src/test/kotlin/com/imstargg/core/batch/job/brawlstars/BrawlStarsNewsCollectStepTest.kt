package com.imstargg.core.batch.job.brawlstars

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.news.NewsArticleContentExtractor
import com.imstargg.core.batch.domain.news.NewsArticleTypeClassifier
import com.imstargg.core.batch.job.brawlstars.task.BrawlStarsNewsCollectTask
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.blog.BrawlStarsBlogArticleSummary
import com.imstargg.core.batch.support.blog.BrawlStarsBlogClient
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.BrawlStarsNewsArticleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import java.time.Instant

/**
 * 진짜 [BrawlStarsNewsCollectTask] 를 진짜 스텝 안에서 돌린다.
 *
 * 스텝에 트랜잭션 매니저를 넘기면 알림이 던질 때 저장한 행까지 롤백되고, 다음 실행이 같은 알림을 다시 보낸다.
 * 스텝 안에서도 저장이 따로 커밋되는 성질을 여기서 고정한다.
 */
class BrawlStarsNewsCollectStepTest(
    private val jobOperator: JobOperator,
    @param:Qualifier(BrawlStarsNewsCollectJobConfig.JOB_NAME) private val brawlStarsNewsCollectJob: Job,
    private val repository: BrawlStarsNewsArticleJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @MockkBean
    private lateinit var blogClient: BrawlStarsBlogClient

    @MockkBean(relaxUnitFun = true)
    private lateinit var alertManager: AlertManager

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `알림 전송이 실패하면 잡은 실패하고 저장한 행은 남는다`() {
        // given - 글 하나를 미리 저장해 첫 실행이 아니고, 목록에 새 글 하나가 더 있다
        val saved = Doc(slug = "saved", publishedAt = Instant.parse("2026-06-01T00:00:00Z"))
        val fresh = Doc(slug = "fresh", publishedAt = Instant.parse("2026-06-10T00:00:00Z"))
        repository.save(
            BrawlStarsNewsArticleEntity(
                link = saved.link,
                title = saved.title,
                publishedAt = saved.publishedAt,
                type = NewsArticleTypeClassifier.classify(saved.title),
                rawBody = saved.rawBody,
                content = NewsArticleContentExtractor.extract(saved.rawBody),
            ),
        )
        val rawBodies = listOf(saved, fresh).associate { it.link.linkUrl to it.rawBody }
        every { blogClient.getArticleSummaries(1) } returns listOf(fresh.summary(), saved.summary())
        every { blogClient.getArticleSummaries(2) } returns emptyList()
        every { blogClient.getArticleSummaries(3) } returns emptyList()
        every { blogClient.getRawBody(any()) } answers {
            rawBodies.getValue(firstArg<BrawlStarsNewsArticleLink>().linkUrl)
        }
        every { alertManager.alert(any()) } throws IllegalStateException("Discord 알림 전송 실패")

        // when
        val execution = jobOperator.start(brawlStarsNewsCollectJob, JobParameters())

        // then
        assertThat(execution.status).isEqualTo(BatchStatus.FAILED)
        verify(exactly = 1) { alertManager.alert(any()) }
        assertThat(repository.findAll().map { it.link.linkUrl })
            .containsExactlyInAnyOrder(saved.link.linkUrl, fresh.link.linkUrl)
    }

    private data class Doc(
        val slug: String,
        val publishedAt: Instant,
    ) {
        val title: String = "제목 $slug"

        val link: BrawlStarsNewsArticleLink = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/news/$slug")

        val rawBody: String =
            """[{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[""" +
                """{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"본문 $slug","marks":[],"data":{}}]}""" +
                "]}}}]"

        fun summary() = BrawlStarsBlogArticleSummary(title = title, link = link, publishedAt = publishedAt)
    }
}
