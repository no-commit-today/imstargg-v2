package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GameSlotTest {
    @Nested
    inner class ResultSlotsPerGame {
        @Test
        fun `3v3 은 6 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).resultSlotsPerGame).isEqualTo(6)
        }

        @Test
        fun `솔로 쇼다운 10팀은 10 슬롯`() {
            assertThat(GameSlot(List(10) { 1 }).resultSlotsPerGame).isEqualTo(10)
        }

        @Test
        fun `비대칭 1v5 는 6 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).resultSlotsPerGame).isEqualTo(6)
        }
    }

    @Nested
    inner class PairSlotsPerGame {
        @Test
        fun `3v3 은 12 페어 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).pairSlotsPerGame).isEqualTo(12)
        }

        @Test
        fun `솔로 쇼다운 10팀은 페어가 0`() {
            assertThat(GameSlot(List(10) { 1 }).pairSlotsPerGame).isEqualTo(0)
        }

        @Test
        fun `1v5 비대칭은 20 페어 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).pairSlotsPerGame).isEqualTo(20)
        }
    }

    @Nested
    inner class EnemySlotsPerGame {
        @Test
        fun `3v3 은 18 매치업 슬롯`() {
            assertThat(GameSlot(listOf(3, 3)).enemySlotsPerGame).isEqualTo(18)
        }

        @Test
        fun `솔로 쇼다운 10팀은 90 매치업 슬롯`() {
            assertThat(GameSlot(List(10) { 1 }).enemySlotsPerGame).isEqualTo(90)
        }

        @Test
        fun `1v5 비대칭은 10 매치업 슬롯`() {
            assertThat(GameSlot(listOf(1, 5)).enemySlotsPerGame).isEqualTo(10)
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `default 는 BattleEventMode 의 DEFAULT_PLAYER_COUNTS 와 일치`() {
            assertThat(GameSlot.default().playerCounts).isEqualTo(BattleEventMode.DEFAULT_PLAYER_COUNTS)
        }
    }
}
