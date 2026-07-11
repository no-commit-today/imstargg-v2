package com.imstargg.core.batch.job.brawlstars.task

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.error.YAMLException

class YamlCanonicalizerTest {
    @Test
    fun `맵 키 순서만 다른 두 YAML 은 동일한 정규화본이 된다`() {
        // given
        val yamlA = "swagger: \"2.0\"\ninfo:\n  title: Brawl Stars API\n  version: v1"
        val yamlB = "info:\n  version: v1\n  title: Brawl Stars API\nswagger: \"2.0\""

        // when
        val canonicalA = YamlCanonicalizer.canonicalize(yamlA)
        val canonicalB = YamlCanonicalizer.canonicalize(yamlB)

        // then
        assertThat(canonicalA).isEqualTo(canonicalB)
    }

    @Test
    fun `중첩 맵 키까지 재귀 정렬하고 스칼라 표기를 보존한다`() {
        // given
        val yaml = "b:\n  d: 2\n  c: 1\na: 3\nswagger: \"2.0\""

        // when
        val canonical = YamlCanonicalizer.canonicalize(yaml)

        // then
        assertThat(canonical).isEqualTo("a: 3\nb:\n  c: 1\n  d: 2\nswagger: '2.0'\n")
    }

    @Test
    fun `리스트 순서는 보존한다`() {
        // given
        val yamlA = "schemes:\n  - https\n  - http"
        val yamlB = "schemes:\n  - http\n  - https"

        // when
        val canonicalA = YamlCanonicalizer.canonicalize(yamlA)
        val canonicalB = YamlCanonicalizer.canonicalize(yamlB)

        // then
        assertThat(canonicalA).isEqualTo("schemes:\n- https\n- http\n")
        assertThat(canonicalA).isNotEqualTo(canonicalB)
    }

    @Test
    fun `유효하지 않은 YAML 이면 예외가 발생한다`() {
        // when
        val thrown =
            catchThrowableOfType(YAMLException::class.java) {
                YamlCanonicalizer.canonicalize("key: [unclosed")
            }

        // then
        assertThat(thrown).isNotNull()
    }
}
