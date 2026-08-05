package com.imstargg.infrastructure.db.core.statistics.brawler

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class OwnershipCountEmbeddedTest {
    @Test
    fun `소유 수가 전체 수 이하면 생성된다`() {
        val sut = OwnershipCountEmbedded(ownerCount = 3, totalCount = 10)

        assertThat(sut.ownerCount).isEqualTo(3)
        assertThat(sut.totalCount).isEqualTo(10)
    }

    @Test
    fun `아무도 소유하지 않아도 생성된다`() {
        val sut = OwnershipCountEmbedded(ownerCount = 0, totalCount = 0)

        assertThat(sut.ownerCount).isZero()
        assertThat(sut.totalCount).isZero()
    }

    @Test
    fun `소유 수가 전체 수보다 크면 예외가 발생한다`() {
        val result =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                OwnershipCountEmbedded(ownerCount = 11, totalCount = 10)
            }

        assertThat(result).hasMessageContaining("11").hasMessageContaining("10")
    }

    @Test
    fun `소유 수가 음수면 예외가 발생한다`() {
        val result =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                OwnershipCountEmbedded(ownerCount = -1, totalCount = 10)
            }

        assertThat(result).hasMessageContaining("-1")
    }
}
