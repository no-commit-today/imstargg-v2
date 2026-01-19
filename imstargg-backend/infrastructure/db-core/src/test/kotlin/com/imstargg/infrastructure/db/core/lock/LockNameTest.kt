package com.imstargg.infrastructure.db.core.lock

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LockNameTest {
    @Nested
    inner class Creation {
        @Test
        fun `유효한 이름으로 생성하면 성공한다`() {
            // given
            val value = "player:ABC123"

            // when
            val sut = LockName(value)

            // then
            assertThat(sut.value).isEqualTo(value)
        }

        @Test
        fun `64자 이름으로 생성하면 성공한다`() {
            // given
            val value = "a".repeat(64)

            // when
            val sut = LockName(value)

            // then
            assertThat(sut.value).isEqualTo(value)
        }

        @Test
        fun `빈 문자열로 생성하면 IllegalArgumentException이 발생한다`() {
            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName("")
                }

            // then
            assertThat(exception.message).contains("비어있을 수 없습니다")
        }

        @Test
        fun `공백만 있는 문자열로 생성하면 IllegalArgumentException이 발생한다`() {
            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName("   ")
                }

            // then
            assertThat(exception.message).contains("비어있을 수 없습니다")
        }

        @Test
        fun `공백이 포함된 문자열로 생성하면 IllegalArgumentException이 발생한다`() {
            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName("player lock")
                }

            // then
            assertThat(exception.message).contains("공백을 포함할 수 없습니다")
        }

        @Test
        fun `탭이 포함된 문자열로 생성하면 IllegalArgumentException이 발생한다`() {
            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName("player\tlock")
                }

            // then
            assertThat(exception.message).contains("공백을 포함할 수 없습니다")
        }

        @Test
        fun `개행이 포함된 문자열로 생성하면 IllegalArgumentException이 발생한다`() {
            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName("player\nlock")
                }

            // then
            assertThat(exception.message).contains("공백을 포함할 수 없습니다")
        }

        @Test
        fun `65자 이상 이름으로 생성하면 IllegalArgumentException이 발생한다`() {
            // given
            val value = "a".repeat(65)

            // when
            val exception =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    LockName(value)
                }

            // then
            assertThat(exception.message).contains("64자를 초과할 수 없습니다")
        }
    }

    @Nested
    inner class CreatePlayerLockName {
        @Test
        fun `태그로 플레이어 락 이름을 생성한다`() {
            // given
            val tag = "ABC123"

            // when
            val sut = LockName.createPlayerLockName(tag)

            // then
            assertThat(sut.value).isEqualTo("player:ABC123")
        }
    }

    @Nested
    inner class CreateClubLockName {
        @Test
        fun `태그로 클럽 락 이름을 생성한다`() {
            // given
            val tag = "CLUB456"

            // when
            val sut = LockName.createClubLockName(tag)

            // then
            assertThat(sut.value).isEqualTo("club:CLUB456")
        }
    }
}
