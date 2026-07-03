package com.imstargg.core.batch.job.brawlstars.task

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * YAML 문서를 의미 보존 정규형(맵 키 재귀 정렬)으로 재직렬화한다.
 *
 * 브롤스타즈 API 스펙(`GET /v1`) 응답은 요청마다 맵 키 순서가 달라지므로, 원문 대신
 * 정규화본을 비교·저장 기준으로 삼는다. 정렬 비교는 문자열 기준이지만 키 타입 자체는
 * 보존하고, 리스트 순서는 의미가 있으므로(파라미터 순서 등) 정렬하지 않는다.
 * 유효한 YAML 이 아니면 SnakeYAML 예외를 그대로 전파한다 — 응답 형식 자체의 변경은
 * 잡 실패로 시끄럽게 드러나는 것이 의도된 동작이다.
 */
object YamlCanonicalizer {
    fun canonicalize(content: String): String {
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Any?>(content)
        val dumperOptions =
            DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isAllowUnicode = true
                width = 4096
            }
        return Yaml(dumperOptions).dump(sortMapKeys(root))
    }

    private fun sortMapKeys(node: Any?): Any? =
        when (node) {
            is Map<*, *> ->
                node.entries
                    .sortedBy { it.key.toString() }
                    .associate { it.key to sortMapKeys(it.value) }
            is List<*> -> node.map { sortMapKeys(it) }
            else -> node
        }
}
