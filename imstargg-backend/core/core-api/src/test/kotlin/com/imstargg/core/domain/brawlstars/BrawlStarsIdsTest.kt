package com.imstargg.core.domain.brawlstars

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlStarsIdsTest {
    @Test
    fun `of 는 value 기준 오름차순 정렬을 보장`() {
        // when
        val sut =
            BrawlStarsIds.of(
                listOf(
                    BrawlStarsId(16000003L),
                    BrawlStarsId(16000001L),
                    BrawlStarsId(16000002L),
                ),
            )

        // then
        assertThat(sut.values.map { it.value })
            .containsExactly(16000001L, 16000002L, 16000003L)
    }

    @Test
    fun `key 는 정렬된 value 를 _ 로 join`() {
        // when
        val sut =
            BrawlStarsIds.of(
                listOf(
                    BrawlStarsId(16000003L),
                    BrawlStarsId(16000001L),
                    BrawlStarsId(16000002L),
                ),
            )

        // then
        assertThat(sut.key()).isEqualTo("16000001_16000002_16000003")
    }

    @Test
    fun `key 는 입력 순서가 달라도 같은 결과`() {
        // when
        val a = BrawlStarsIds.of(listOf(BrawlStarsId(2L), BrawlStarsId(1L)))
        val b = BrawlStarsIds.of(listOf(BrawlStarsId(1L), BrawlStarsId(2L)))

        // then
        assertThat(a.key()).isEqualTo(b.key())
    }

    @Test
    fun `빈 리스트면 key 는 빈 문자열이고 isEmpty 가 true`() {
        // when
        val sut = BrawlStarsIds.of(emptyList())

        // then
        assertThat(sut.isEmpty()).isTrue()
        assertThat(sut.key()).isEqualTo("")
        assertThat(sut.size).isEqualTo(0)
    }
}
