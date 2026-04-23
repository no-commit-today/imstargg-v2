package com.imstargg.infrastructure.db.core.statistics.battle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BattleKeyEmbeddedTest {
    private val battleTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime()
    private val anotherTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 2, 0, 0), ZoneId.systemDefault()).toOffsetDateTime()

    @Nested
    inner class forResult {
        @Test
        fun `길이64의 배틀키를 생성한다`() {
            val sut = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))

            assertThat(sut.value).hasSize(64)
        }

        @Test
        fun `전투 시간과 플레이어가 같으면 같은 키를 생성한다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))

            assertThat(result1).isEqualTo(result2)
        }

        @Test
        fun `플레이어 태그 순서는 달라도 된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player2", "player1", "player3"))

            assertThat(result1).isEqualTo(result2)
        }

        @Test
        fun `날짜가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(anotherTime, listOf("player1", "player2", "player3"))

            assertThat(result1).isNotEqualTo(result2)
        }

        @Test
        fun `플레이어 수가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2"))

            assertThat(result1).isNotEqualTo(result2)
        }

        @Test
        fun `플레이어가 다르면 다른 키가 생성된다`() {
            val result1 = BattleKeyEmbedded.forResult(battleTime, listOf("player1", "player2", "player3"))
            val result2 = BattleKeyEmbedded.forResult(battleTime, listOf("player4", "player5", "player6"))

            assertThat(result1).isNotEqualTo(result2)
        }
    }

    @Nested
    inner class forRank {
        @Test
        fun `자기 팀 태그만으로 키를 생성한다`() {
            val myTeam = listOf("A", "B", "C")

            val sut = BattleKeyEmbedded.forRank(battleTime, myTeam)

            assertThat(sut.value).hasSize(64)
        }

        @Test
        fun `같은 팀의 다른 수집 주체는 같은 키를 생성한다`() {
            val myTeam = listOf("A", "B", "C")

            val fromA = BattleKeyEmbedded.forRank(battleTime, myTeam)
            val fromB = BattleKeyEmbedded.forRank(battleTime, myTeam)

            assertThat(fromA).isEqualTo(fromB)
        }

        @Test
        fun `다른 팀은 다른 키를 생성한다`() {
            val ourTeam = listOf("A", "B", "C")
            val enemyTeam = listOf("D", "E", "F")

            val ours = BattleKeyEmbedded.forRank(battleTime, ourTeam)
            val theirs = BattleKeyEmbedded.forRank(battleTime, enemyTeam)

            assertThat(ours).isNotEqualTo(theirs)
        }
    }

    @Nested
    inner class forResult_vs_forRank {
        @Test
        fun `같은 태그 집합이어도 forResult 와 forRank 는 같은 hash 를 반환한다`() {
            val tags = listOf("A", "B", "C")

            val asResult = BattleKeyEmbedded.forResult(battleTime, tags)
            val asRank = BattleKeyEmbedded.forRank(battleTime, tags)

            assertThat(asResult).isEqualTo(asRank)
        }
    }
}
