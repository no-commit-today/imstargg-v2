package com.imstargg.core.batch.support.blog

import com.imstargg.infrastructure.db.core.brawlstars.news.BrawlStarsNewsArticleLink
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class BrawlStarsBlogClientTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val sut = BrawlStarsBlogClient(builder)

    private val link =
        BrawlStarsNewsArticleLink.of(
            "/en/games/brawlstars/blog/game-updates/release-notes-november-2026",
        )

    @Test
    fun `목록 1쪽을 읽는다`() {
        // given
        server
            .expect(requestTo("https://supercell.com/en/games/brawlstars/blog/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(resource("list-page-1.html"), MediaType.TEXT_HTML))

        // when
        val summaries = sut.getArticleSummaries(1)

        // then
        assertThat(summaries).hasSize(6)
        assertThat(summaries[0].title).isEqualTo("Release Notes November 2026")
        assertThat(summaries[0].link.linkUrl)
            .isEqualTo("https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026")
        assertThat(summaries[0].publishedAt).isEqualTo(Instant.parse("2026-11-11T03:00:00Z"))
        assertThat(summaries[1].publishedAt).isEqualTo(Instant.parse("2026-10-23T08:16:49.217Z"))
        server.verify()
    }

    @Test
    fun `목록 2쪽은 page 경로로 요청한다`() {
        // given
        server
            .expect(requestTo("https://supercell.com/en/games/brawlstars/blog/page/2/"))
            .andRespond(withSuccess(resource("list-page-1.html"), MediaType.TEXT_HTML))

        // when
        sut.getArticleSummaries(2)

        // then
        server.verify()
    }

    @Test
    fun `글 본문을 JSON 문자열로 돌려준다`() {
        // given
        server
            .expect(requestTo(link.linkUrl + "/"))
            .andRespond(
                withSuccess(
                    resource("article-release-notes-november-2026.masked.html"),
                    MediaType.TEXT_HTML,
                ),
            )

        // when
        val rawBody = sut.getRawBody(link)

        // then
        val body = JsonMapper.builder().build().readTree(rawBody)
        assertThat(body.isArray).isTrue()
        assertThat(body.size()).isEqualTo(10)
        server.verify()
    }

    @Test
    fun `문자 인코딩 표시가 없어도 한글과 이모지를 읽는다`() {
        // given
        val html = articleHtml("""[{"type":"TextBlock","title":"한글 ⚖️"}]""")
        server
            .expect(requestTo(link.linkUrl + "/"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.parseMediaType("text/html"))
                    .body(html.toByteArray(Charsets.UTF_8)),
            )

        // when
        val rawBody = sut.getRawBody(link)

        // then
        assertThat(rawBody).contains("한글 ⚖️")
    }

    @Test
    fun `NEXT_DATA 가 없으면 예외다`() {
        // given
        server
            .expect(requestTo("https://supercell.com/en/games/brawlstars/blog/"))
            .andRespond(withSuccess("<html><body>없음</body></html>", MediaType.TEXT_HTML))

        // when & then
        assertThatThrownBy { sut.getArticleSummaries(1) }
            .isInstanceOf(BrawlStarsBlogPageException::class.java)
            .hasMessageContaining("https://supercell.com/en/games/brawlstars/blog/")
            .hasMessageContaining("__NEXT_DATA__")
    }

    @Test
    fun `목록에 글이 없으면 예외다`() {
        // given
        server
            .expect(requestTo("https://supercell.com/en/games/brawlstars/blog/"))
            .andRespond(
                withSuccess(
                    nextDataHtml("""{"props":{"pageProps":{"articles":[]}},"page":"/en/games/brawlstars/blog"}"""),
                    MediaType.TEXT_HTML,
                ),
            )

        // when & then
        assertThatThrownBy { sut.getArticleSummaries(1) }
            .isInstanceOf(BrawlStarsBlogPageException::class.java)
            .hasMessageContaining("목록에 글이 없다")
    }

    @Test
    fun `게시일이 없는 글이 목록에 있으면 예외다`() {
        // given
        server
            .expect(requestTo("https://supercell.com/en/games/brawlstars/blog/"))
            .andRespond(
                withSuccess(
                    nextDataHtml(
                        """{"props":{"pageProps":{"articles":[{"title":"t","linkUrl":"/en/games/brawlstars/blog/a/b"}]}}}""",
                    ),
                    MediaType.TEXT_HTML,
                ),
            )

        // when & then
        assertThatThrownBy { sut.getArticleSummaries(1) }
            .isInstanceOf(BrawlStarsBlogPageException::class.java)
            .hasMessageContaining("게시일")
    }

    @Test
    fun `없는 글의 페이지는 예외다`() {
        // given
        server
            .expect(requestTo(link.linkUrl + "/"))
            .andRespond(
                withSuccess(
                    nextDataHtml("""{"props":{"pageProps":{}},"page":"/404"}"""),
                    MediaType.TEXT_HTML,
                ),
            )

        // when & then
        assertThatThrownBy { sut.getRawBody(link) }
            .isInstanceOf(BrawlStarsBlogPageException::class.java)
            .hasMessageContaining(link.linkUrl)
            .hasMessageContaining("/404")
    }

    @Test
    fun `글 페이지에 본문이 없으면 예외다`() {
        // given
        server
            .expect(requestTo(link.linkUrl + "/"))
            .andRespond(withSuccess(articleHtml("[]"), MediaType.TEXT_HTML))

        // when & then
        assertThatThrownBy { sut.getRawBody(link) }
            .isInstanceOf(BrawlStarsBlogPageException::class.java)
            .hasMessageContaining("글 본문이 없다")
    }

    @Test
    fun `서버 오류는 그대로 던진다`() {
        // given
        server
            .expect(requestTo(link.linkUrl + "/"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        // when & then
        assertThatThrownBy { sut.getRawBody(link) }
            .isInstanceOf(HttpServerErrorException::class.java)
    }

    private fun articleHtml(bodyCollectionJson: String): String =
        nextDataHtml(
            """{"props":{"pageProps":{"bodyCollection":$bodyCollectionJson}},""" +
                """"page":"/en/games/brawlstars/blog/[category]/[title]"}""",
        )

    private fun nextDataHtml(json: String): String =
        """<html><body><script id="__NEXT_DATA__" type="application/json">$json</script></body></html>"""

    private fun resource(name: String): String = ClassPathResource("brawlstars-blog/$name").getContentAsString(Charsets.UTF_8)
}
