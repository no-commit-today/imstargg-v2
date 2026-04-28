package com.imstargg.core.config

import com.imstargg.core.enums.Country
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StringToCountryConverterTest {
    private val sut = StringToCountryConverter()

    @Nested
    inner class `code 로 변환` {
        @Test
        fun `소문자 code 가 매칭된다`() {
            // when
            val result = sut.convert("kr")

            // then
            assertThat(result).isEqualTo(Country.KOREA)
        }

        @Test
        fun `대소문자 혼합도 매칭된다`() {
            // when
            val result = sut.convert("KR")

            // then
            assertThat(result).isEqualTo(Country.KOREA)
        }

        @Test
        fun `global code 가 매칭된다`() {
            // when
            val result = sut.convert("global")

            // then
            assertThat(result).isEqualTo(Country.GLOBAL)
        }

        @Test
        fun `앞뒤 공백을 무시한다`() {
            // when
            val result = sut.convert(" jp ")

            // then
            assertThat(result).isEqualTo(Country.JAPAN)
        }
    }

    @Nested
    inner class `enum name 으로 변환` {
        @Test
        fun `대문자 enum 이름이 매칭된다`() {
            // when
            val result = sut.convert("KOREA")

            // then
            assertThat(result).isEqualTo(Country.KOREA)
        }
    }

    @Nested
    inner class `매칭 실패` {
        @Test
        fun `알 수 없는 값은 IllegalArgumentException`() {
            // when
            val ex =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    sut.convert("fr")
                }

            // then
            assertThat(ex).hasMessageContaining("fr")
        }

        @Test
        fun `빈 문자열은 IllegalArgumentException`() {
            // when
            val ex =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    sut.convert("   ")
                }

            // then
            assertThat(ex).isNotNull()
        }
    }
}
