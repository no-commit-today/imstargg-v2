package com.imstargg.core.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SlotsPerGameTest {
    @Nested
    inner class ForResult {
        @Test
        fun `3v3 은 6 슬롯`() {
            assertThat(SlotsPerGame.forResult(listOf(3, 3))).isEqualTo(6)
        }

        @Test
        fun `솔로 쇼다운 10팀은 10 슬롯`() {
            assertThat(SlotsPerGame.forResult(List(10) { 1 })).isEqualTo(10)
        }

        @Test
        fun `비대칭 1v5는 6 슬롯`() {
            assertThat(SlotsPerGame.forResult(listOf(1, 5))).isEqualTo(6)
        }
    }

    @Nested
    inner class ForPair {
        @Test
        fun `3v3 은 12 페어 슬롯`() {
            assertThat(SlotsPerGame.forPair(listOf(3, 3))).isEqualTo(12)
        }

        @Test
        fun `솔로 쇼다운 10팀은 페어가 0`() {
            assertThat(SlotsPerGame.forPair(List(10) { 1 })).isEqualTo(0)
        }

        @Test
        fun `1v5 비대칭은 20 페어 슬롯`() {
            assertThat(SlotsPerGame.forPair(listOf(1, 5))).isEqualTo(20)
        }
    }

    @Nested
    inner class ForEnemy {
        @Test
        fun `3v3 은 18 매치업 슬롯`() {
            assertThat(SlotsPerGame.forEnemy(listOf(3, 3))).isEqualTo(18)
        }

        @Test
        fun `솔로 쇼다운 10팀은 90 매치업 슬롯`() {
            assertThat(SlotsPerGame.forEnemy(List(10) { 1 })).isEqualTo(90)
        }

        @Test
        fun `1v5 비대칭은 10 매치업 슬롯`() {
            assertThat(SlotsPerGame.forEnemy(listOf(1, 5))).isEqualTo(10)
        }
    }
}
