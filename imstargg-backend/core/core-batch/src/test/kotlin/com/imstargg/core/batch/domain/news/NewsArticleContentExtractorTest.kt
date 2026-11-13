package com.imstargg.core.batch.domain.news

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.json.JsonMapper

class NewsArticleContentExtractorTest {

    @Test
    fun `글 블록은 제목 한 줄 뒤에 문단과 소제목과 목록 항목을 한 줄씩 쓴다`() {
        val body = """
            [{"__typename":"TextBlock","title":"제목","text":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"heading-2","data":{},"content":[{"nodeType":"text","value":"소제목","marks":[],"data":{}}]},
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"문단","marks":[],"data":{}}]},
              {"nodeType":"unordered-list","data":{},"content":[
                {"nodeType":"list-item","data":{},"content":[{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"항목1","marks":[],"data":{}}]}]},
                {"nodeType":"list-item","data":{},"content":[{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"항목2","marks":[],"data":{}}]}]}
              ]}
            ]}}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("제목\n소제목\n문단\n항목1\n항목2")
    }

    @Test
    fun `기능 소개 블록은 제목 한 줄 뒤에 본문을 쓴다`() {
        val body = """
            [{"__typename":"FeatureBlock","title":"기능","featureText":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"설명","marks":[],"data":{}}]}
            ]}},"featureThumbnail":{"url":"x"}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("기능\n설명")
    }

    @Test
    fun `이미지와 영상 블록은 버린다`() {
        val body = """[{"__typename":"ImageBlock","title":"그림"},{"__typename":"VideoBlock","title":"영상"}]"""

        assertThat(NewsArticleContentExtractor.extract(body)).isEmpty()
    }

    @Test
    fun `모르는 블록은 종류를 적은 한 줄을 남긴다`() {
        val body = """[{"__typename":"QuizBlock"}]"""

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("[?QuizBlock]")
    }

    @Test
    fun `표의 행은 칸을 공백으로 이어 한 줄로 쓴다`() {
        val body = """
            [{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"table","data":{},"content":[
                {"nodeType":"table-row","data":{},"content":[
                  {"nodeType":"table-header-cell","data":{},"content":[{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"A","marks":[],"data":{}}]}]},
                  {"nodeType":"table-cell","data":{},"content":[{"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"B","marks":[],"data":{}}]}]}
                ]}
              ]}
            ]}}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("A B")
    }

    @Test
    fun `링크와 굵게와 기울임은 글자만 남긴다`() {
        val body = """
            [{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"paragraph","data":{},"content":[
                {"nodeType":"text","value":"앞 ","marks":[{"type":"bold"}],"data":{}},
                {"nodeType":"hyperlink","data":{"uri":"https://example.com"},"content":[{"nodeType":"text","value":"링크","marks":[{"type":"italic"}],"data":{}}]},
                {"nodeType":"text","value":" 뒤","marks":[],"data":{}}
              ]}
            ]}}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("앞 링크 뒤")
    }

    @Test
    fun `문단 안의 줄바꿈에서 줄을 나눈다`() {
        val body = """
            [{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"a\nb\r\nc\rd","marks":[],"data":{}}]}
            ]}}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("a\nb\nc\nd")
    }

    @Test
    fun `특수 공백은 공백으로 바꾸고 연속 공백은 줄이고 빈 줄은 버린다`() {
        val body = """
            [{"__typename":"TextBlock","title":"","text":{"json":{"nodeType":"document","data":{},"content":[
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"a  b 　c\td","marks":[],"data":{}}]},
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"   ","marks":[],"data":{}}]},
              {"nodeType":"paragraph","data":{},"content":[{"nodeType":"text","value":"﻿끝 ","marks":[],"data":{}}]}
            ]}}}]
        """.trimIndent()

        assertThat(NewsArticleContentExtractor.extract(body)).isEqualTo("a b c d\n끝")
    }

    @Test
    fun `실제 글의 사본에서 뽑은 결과가 고정돼 있다`() {
        val html = ClassPathResource("brawlstars-blog/article-release-notes-november-2026.masked.html")
            .getContentAsString(Charsets.UTF_8)
        val rawBody = bodyCollectionOf(html)

        val content = NewsArticleContentExtractor.extract(rawBody)

        val lines = content.lines()
        assertThat(lines).hasSize(635)
        assertThat(lines[0]).isEqualTo("xxxxxxxxxxx - xxxxxxxx 00")
        assertThat(lines[2]).isEqualTo("⚖️ xxxxxxx xxxxxxx ⚖️")
        assertThat(lines.last()).isEqualTo("xxxxx xxxx xxxxxx xxx xxxxxxx xxx xxxxxxx xx xxxx xxxxx xxxxxxx xxxxx.")
        assertThat(content.length).isEqualTo(43222)
        assertThat(lines).noneMatch { it.isBlank() || it.contains("  ") || it != it.trim() }
    }

    private fun bodyCollectionOf(html: String): String {
        val match = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html) ?: error("__NEXT_DATA__ 가 없다")
        val root = JsonMapper.builder().build().readTree(match.groupValues[1])
        return root.path("props").path("pageProps").path("bodyCollection").toString()
    }
}
