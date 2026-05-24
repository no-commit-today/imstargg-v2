package com.imstargg.core.domain.brawlstars.player

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class ClubSearchTest {
    @Test
    fun `of 는 raw 입력을 trim 한 값을 query 로 보관한다`() {
        // when
        val sut = ClubSearch.of("  뉴비  ")

        // then
        assertThat(sut.query).isEqualTo("뉴비")
    }

    @Test
    fun `of 는 빈 문자열에 대해 IllegalArgumentException 을 던진다`() {
        // when
        val ex =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                ClubSearch.of("")
            }

        // then
        assertThat(ex).isNotNull
    }

    @Test
    fun `of 는 공백만으로 구성된 문자열에 대해 IllegalArgumentException 을 던진다`() {
        // when
        val ex =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                ClubSearch.of("   ")
            }

        // then
        assertThat(ex).isNotNull
    }
}
