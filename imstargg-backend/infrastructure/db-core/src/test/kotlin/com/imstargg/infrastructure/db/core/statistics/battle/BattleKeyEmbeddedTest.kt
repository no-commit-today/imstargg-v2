package com.imstargg.infrastructure.db.core.statistics.battle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BattleKeyEmbeddedTest {
    private val battleTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toInstant()
    private val anotherTime =
        ZonedDateTime.of(LocalDateTime.of(2021, 1, 2, 0, 0), ZoneId.systemDefault()).toInstant()

    @Test
    fun `길이64의 배틀키를 생성한다`() {
        val sut = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))

        assertThat(sut.value).hasSize(64)
    }

    @Test
    fun `전투 시간과 플레이어가 같으면 같은 키를 생성한다`() {
        val result1 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))
        val result2 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `플레이어 태그 순서는 달라도 된다`() {
        val result1 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))
        val result2 = BattleKeyEmbedded.create(battleTime, listOf("player2", "player1", "player3"))

        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `날짜가 다르면 다른 키가 생성된다`() {
        val result1 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))
        val result2 = BattleKeyEmbedded.create(anotherTime, listOf("player1", "player2", "player3"))

        assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `플레이어 수가 다르면 다른 키가 생성된다`() {
        val result1 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))
        val result2 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2"))

        assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `플레이어가 다르면 다른 키가 생성된다`() {
        val result1 = BattleKeyEmbedded.create(battleTime, listOf("player1", "player2", "player3"))
        val result2 = BattleKeyEmbedded.create(battleTime, listOf("player4", "player5", "player6"))

        assertThat(result1).isNotEqualTo(result2)
    }
}
