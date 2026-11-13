package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.news.NewsArticleContentExtractor
import com.imstargg.core.batch.domain.news.NewsArticleTypeClassifier
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.blog.BrawlStarsBlogArticleSummary
import com.imstargg.core.batch.support.blog.BrawlStarsBlogClient
import com.imstargg.core.enums.BrawlStarsNewsArticleType
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleEntity
import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import com.imstargg.infrastructure.db.core.brawlstars.news.repository.BrawlStarsNewsArticleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant

class BrawlStarsNewsCollectTaskTest(
    private val sut: BrawlStarsNewsCollectTask,
    private val repository: BrawlStarsNewsArticleJpaRepository,
    private val cleanUp: CleanUp,
    private val clock: Clock,
) : CoreBatchContextTest() {
    @MockkBean
    private lateinit var blogClient: BrawlStarsBlogClient

    @MockkBean(relaxUnitFun = true)
    private lateinit var alertManager: AlertManager

    private val rawBodies = mutableMapOf<String, String>()
    private val failingUrls = mutableSetOf<String>()
    private val alerts = mutableListOf<AlertCommand>()

    @BeforeEach
    fun setUp() {
        every { alertManager.alert(capture(alerts)) } just runs
        every { blogClient.getRawBody(any()) } answers {
            val url = firstArg<BrawlStarsNewsArticleLink>().linkUrl
            check(url !in failingUrls) { "본문을 받지 못했다 - $url" }
            rawBodies.getValue(url)
        }
    }

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `첫 실행은 모든 글을 게시일이 이른 순서로 저장하고 요약 알림 하나만 보낸다`() {
        // given
        val a = doc("a", days = 3)
        val b = doc("b", days = 1)
        val c = doc("c", days = 2)
        serve(listOf(a), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.findAll().sortedBy { it.id }.map { it.link.linkUrl })
            .containsExactly(b.link.linkUrl, c.link.linkUrl, a.link.linkUrl)
        assertThat(alerts).hasSize(1)
        assertThat(alerts.single().title).startsWith("[첫 실행]")
    }

    @Test
    fun `첫 실행이 중간에 실패하면 한 건도 남지 않는다`() {
        // given - 게시일이 가장 늦은 글이 세 번째로 처리된다
        val a = doc("a", days = 3)
        val b = doc("b", days = 1)
        val c = doc("c", days = 2)
        serve(listOf(a), listOf(b), listOf(c))
        failingUrls += a.link.linkUrl

        // when & then
        assertThatThrownBy { sut.collect() }.isInstanceOf(IllegalStateException::class.java)
        assertThat(repository.count()).isZero()
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `변경이 없으면 저장도 알림도 없다`() {
        // given
        val (a, b, c) = seedBase()
        serve(listOf(a), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(3)
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `새 글은 저장하고 새 글 알림을 보낸다`() {
        // given
        val (a, b, c) = seedBase()
        val fresh = doc("fresh", days = 10)
        serve(listOf(fresh, a), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(4)
        assertThat(
            repository
                .findAll()
                .maxBy { it.id!! }
                .link.linkUrl,
        ).isEqualTo(fresh.link.linkUrl)
        assertThat(alerts.map { it.title }).containsExactly("[새 글] [기타] ${fresh.title}")
    }

    @Test
    fun `본문이 바뀌면 새 판을 저장하고 변경 알림을 보낸다`() {
        // given
        val (a, b, c) = seedBase()
        val edited = a.copy(lines = a.lines + "추가된 문단")
        serve(listOf(edited), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(4)
        val latest = repository.findAll().maxBy { it.id!! }
        assertThat(latest.content).isEqualTo(NewsArticleContentExtractor.extract(edited.rawBody))
        assertThat(alerts).hasSize(1)
        assertThat(alerts.single().title).isEqualTo("[변경] [기타] ${a.title}")
        assertThat(alerts.single().content).contains("본문 1줄 추가, 0줄 삭제")
    }

    @Test
    fun `제목이 바뀌면 새 판을 저장한다`() {
        // given
        val (a, b, c) = seedBase()
        val renamed = a.copy(title = "바뀐 제목")
        serve(listOf(renamed), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(4)
        assertThat(repository.findAll().maxBy { it.id!! }.title).isEqualTo("바뀐 제목")
        assertThat(alerts.single().content).contains("제목 ${a.title} → 바뀐 제목")
    }

    @Test
    fun `주소가 바뀌면 같은 주소 키의 직전 판과 견줘 새 판을 저장한다`() {
        // given - update65-2 가 저장돼 있고 같은 본문이 update65-3 으로 다시 나온다
        val (_, b, c) = seedBase()
        val old = doc("update65-2", title = "업데이트 글", days = 5)
        seed(old)
        val moved = old.copy(slug = "update65-3")
        serve(listOf(moved), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(5)
        assertThat(
            repository
                .findAll()
                .maxBy { it.id!! }
                .link.linkUrl,
        ).isEqualTo(moved.link.linkUrl)
        assertThat(alerts).hasSize(1)
        assertThat(alerts.single().title).startsWith("[변경]")
        assertThat(alerts.single().content).contains("주소 ${old.link.linkUrl} → ${moved.link.linkUrl}")
    }

    @Test
    fun `새 판은 직전 판의 유형을 물려받는다`() {
        // given - 제목은 릴리스 노트로 판정되지만 직전 판은 OTHER 로 고쳐 둔 상태
        val (_, b, c) = seedBase()
        val notes = doc("notes", title = "Brawl Stars Release Notes", days = 5)
        seed(notes, type = BrawlStarsNewsArticleType.OTHER)
        val edited = notes.copy(lines = notes.lines + "추가")
        serve(listOf(edited), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.findAll().maxBy { it.id!! }.type).isEqualTo(BrawlStarsNewsArticleType.OTHER)
        assertThat(alerts.single().title).startsWith("[변경] [기타]")
    }

    @Test
    fun `같은 주소가 두 쪽에 나오면 한 번만 다룬다`() {
        // given
        val (a, b, _) = seedBase()
        val fresh = doc("fresh", days = 10)
        serve(listOf(a, fresh), listOf(fresh, b), emptyList())

        // when
        sut.collect()

        // then
        verify(exactly = 1) { blogClient.getRawBody(fresh.link) }
        assertThat(repository.count()).isEqualTo(4)
        assertThat(alerts).hasSize(1)
    }

    @Test
    fun `같은 주소 키의 두 주소가 함께 있어도 두 번째 실행은 조용하다`() {
        // given
        val (_, b, c) = seedBase()
        val v2 = doc("update65-2", title = "업데이트 글", days = 5)
        val v3 = v2.copy(slug = "update65-3")
        serve(listOf(v2, v3), listOf(b), listOf(c))
        sut.collect()
        val countAfterFirst = repository.count()
        alerts.clear()

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(countAfterFirst)
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `규칙이 바뀌면 저장된 본문만 고치고 알리지 않는다`() {
        // given - 같은 주소 키의 두 판이 지금 규칙과 다른 content 로 저장돼 있다
        val (_, b, c) = seedBase()
        val v2 = doc("update65-2", title = "업데이트 글", days = 5)
        val v3 = v2.copy(slug = "update65-3")
        seed(v2, content = "옛 규칙의 글자")
        seed(v3, content = "옛 규칙의 글자")
        serve(listOf(v3), listOf(b), listOf(c))

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(5)
        assertThat(repository.findAll().filter { it.link.articleKey == "update65" }.map { it.content })
            .containsOnly(NewsArticleContentExtractor.extract(v3.rawBody))
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `글 하나가 실패하면 예외가 나고 그 앞의 저장은 남는다`() {
        // given - 게시일이 늦은 글이 먼저 처리된다
        val (_, b, c) = seedBase()
        val late = doc("late", days = 20)
        val early = doc("early", days = 10)
        serve(listOf(early, late), listOf(b), listOf(c))
        failingUrls += early.link.linkUrl

        // when & then
        assertThatThrownBy { sut.collect() }.isInstanceOf(IllegalStateException::class.java)
        assertThat(repository.findAll().map { it.link.linkUrl }).contains(late.link.linkUrl)
        assertThat(repository.findAll().map { it.link.linkUrl }).doesNotContain(early.link.linkUrl)
        assertThat(repository.count()).isEqualTo(4)
    }

    @Test
    fun `알림 전송이 실패하면 예외가 나고 저장은 남는다`() {
        // given
        val (a, b, c) = seedBase()
        val fresh = doc("fresh", days = 10)
        serve(listOf(fresh, a), listOf(b), listOf(c))
        every { alertManager.alert(any()) } throws IllegalStateException("전송 실패")

        // when & then
        assertThatThrownBy { sut.collect() }.hasMessage("전송 실패")
        assertThat(repository.findAll().map { it.link.linkUrl }).contains(fresh.link.linkUrl)
    }

    @Test
    fun `실패한 실행 뒤에 다시 돌리면 남은 글만 저장하고 알린다`() {
        // given - 첫 번째 실행에서 게시일이 이른 글이 실패한다
        val (_, b, c) = seedBase()
        val late = doc("late", days = 20)
        val early = doc("early", days = 10)
        serve(listOf(early, late), listOf(b), listOf(c))
        failingUrls += early.link.linkUrl
        assertThatThrownBy { sut.collect() }.isInstanceOf(IllegalStateException::class.java)
        alerts.clear()
        failingUrls.clear()

        // when
        sut.collect()

        // then
        assertThat(repository.count()).isEqualTo(5)
        assertThat(alerts.map { it.title }).containsExactly("[새 글] [기타] ${early.title}")
    }

    @Test
    fun `요청 간격이 있어도 수집은 끝까지 간다`() {
        // given
        val task = taskWithInterval(min = 1, max = 1)
        serve(listOf(doc("a", days = 3)), listOf(doc("b", days = 1)), listOf(doc("c", days = 2)))

        // when
        task.collect()

        // then
        assertThat(repository.count()).isEqualTo(3)
    }

    @Test
    fun `요청 간격의 최소가 최대보다 크면 만들 수 없다`() {
        assertThatThrownBy { taskWithInterval(min = 2, max = 1) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun taskWithInterval(
        min: Long,
        max: Long,
    ) = BrawlStarsNewsCollectTask(
        clock = clock,
        blogClient = blogClient,
        repository = repository,
        alertManager = alertManager,
        requestIntervalMinMillis = min,
        requestIntervalMaxMillis = max,
    )

    private data class Doc(
        val slug: String,
        val title: String,
        val publishedAt: Instant,
        val lines: List<String>,
    ) {
        val link: BrawlStarsNewsArticleLink = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/news/$slug")

        val rawBody: String =
            lines.joinToString(
                separator = ",",
                prefix = """[{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[""",
                postfix = "]}}}]",
            ) { line ->
                """{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"$line","marks":[],"data":{}}]}"""
            }

        fun summary() = BrawlStarsBlogArticleSummary(title = title, link = link, publishedAt = publishedAt)
    }

    private fun doc(
        slug: String,
        title: String = "제목 $slug",
        days: Long,
    ) = Doc(
        slug = slug,
        title = title,
        publishedAt = Instant.parse("2026-06-01T00:00:00Z").plusSeconds(days * 86_400),
        lines = listOf("본문 $slug"),
    )

    /** 1~3쪽에 보여줄 글을 정한다. 본문은 글 주소로 찾아 준다. */
    private fun serve(
        page1: List<Doc>,
        page2: List<Doc>,
        page3: List<Doc>,
    ) {
        listOf(page1, page2, page3).forEachIndexed { index, docs ->
            docs.forEach { rawBodies[it.link.linkUrl] = it.rawBody }
            every { blogClient.getArticleSummaries(index + 1) } returns docs.map { it.summary() }
        }
    }

    private fun seed(
        doc: Doc,
        type: BrawlStarsNewsArticleType = NewsArticleTypeClassifier.classify(doc.title),
        content: String = NewsArticleContentExtractor.extract(doc.rawBody),
    ) {
        repository.save(
            BrawlStarsNewsArticleEntity(
                link = doc.link,
                title = doc.title,
                publishedAt = doc.publishedAt,
                type = type,
                rawBody = doc.rawBody,
                content = content,
            ),
        )
    }

    /** 1·2·3쪽에 하나씩 나오는 글 셋을 미리 저장해 첫 실행이 아닌 상태를 만든다. */
    private fun seedBase(): Triple<Doc, Doc, Doc> {
        val docs = Triple(doc("a", days = 3), doc("b", days = 1), doc("c", days = 2))
        seed(docs.first)
        seed(docs.second)
        seed(docs.third)
        return docs
    }
}
