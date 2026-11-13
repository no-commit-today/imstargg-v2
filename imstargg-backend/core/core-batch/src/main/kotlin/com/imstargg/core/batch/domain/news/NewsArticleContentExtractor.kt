package com.imstargg.core.batch.domain.news

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * 공식 블로그 글의 본문 JSON(`bodyCollection`)에서 줄별 글자만 뽑는다.
 *
 * 수집이 직전 판과 본문을 견줄 때 쓰므로, 서식(링크·굵게·기울임)과 이미지·영상은 버리고
 * 화면에서 한 줄로 보이는 글자만 남긴다.
 */
object NewsArticleContentExtractor {

    private val jsonMapper = JsonMapper.builder().build()

    private val LINE_BREAK = Regex("""\r\n|\r|\n""")
    private val SPECIAL_SPACE = Regex("""[   -​  　﻿\t]""")
    private val MULTIPLE_SPACES = Regex(" {2,}")
    private val HEADING_TYPES = (1..6).map { "heading-$it" }.toSet()

    fun extract(rawBody: String): String {
        val lines = mutableListOf<String>()
        jsonMapper.readTree(rawBody).forEach { block -> collectBlock(block, lines) }
        return lines.map(::normalize).filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun collectBlock(block: JsonNode, lines: MutableList<String>) {
        when (val typeName = block.path("__typename").asString("")) {
            "TextBlock" -> {
                lines += block.path("title").asString("")
                collectNode(block.path("text").path("json"), lines)
            }
            "FeatureBlock" -> {
                lines += block.path("title").asString("")
                collectNode(block.path("featureText").path("json"), lines)
            }
            "ImageBlock", "VideoBlock" -> Unit
            else -> lines += "[?$typeName]"
        }
    }

    private fun collectNode(node: JsonNode, lines: MutableList<String>) {
        val nodeType = node.path("nodeType").asString("")
        when {
            nodeType == "paragraph" || nodeType in HEADING_TYPES ->
                lines += concatText(node).split(LINE_BREAK)

            nodeType == "table-row" ->
                lines += node.path("content").joinToString(" ") { cell ->
                    concatText(cell).replace(LINE_BREAK, " ")
                }

            else -> node.path("content").forEach { collectNode(it, lines) }
        }
    }

    private fun concatText(node: JsonNode): String {
        if (node.path("nodeType").asString("") == "text") return node.path("value").asString("")
        return node.path("content").joinToString("") { concatText(it) }
    }

    private fun normalize(line: String): String =
        line.replace(SPECIAL_SPACE, " ").replace(MULTIPLE_SPACES, " ").trim()
}
