package com.imstargg.core.infrastructure.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffsetBasedPageRequestTest {
    @Test
    fun `offset 을 그대로 보존한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 7, limit = 5)

        // when // then
        assertThat(sut.offset).isEqualTo(7L)
        assertThat(sut.pageSize).isEqualTo(5)
        assertThat(sut.pageNumber).isEqualTo(1)
    }

    @Test
    fun `next 는 offset 에 limit 을 더한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 3, limit = 2)

        // when
        val next = sut.next()

        // then
        assertThat(next.offset).isEqualTo(5L)
        assertThat(next.pageSize).isEqualTo(2)
    }

    @Test
    fun `previousOrFirst 는 offset 이 limit 미만이면 first 를 반환한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 1, limit = 5)

        // when
        val previous = sut.previousOrFirst()

        // then
        assertThat(previous.offset).isEqualTo(0L)
    }

    @Test
    fun `withPage 는 pageNumber 곱하기 limit 을 offset 으로 사용한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 3, limit = 10)

        // when
        val paged = sut.withPage(2)

        // then
        assertThat(paged.offset).isEqualTo(20L)
    }
}
