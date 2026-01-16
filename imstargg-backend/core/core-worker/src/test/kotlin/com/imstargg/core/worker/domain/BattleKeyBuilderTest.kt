package com.imstargg.core.worker.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BattleKeyBuilderTest {
    @Test
    fun `길이64의 배틀키를 생성한다`() {
        // given

        // when
        val result =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()

        // then
        Assertions.assertThat(result).hasSize(64)
    }

    @Test
    fun `전투 시간과 플레이어가 같으면 같은 키를 생성한다`() {
        // given

        // when
        val result1 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()
        val result2 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()

        // then
        Assertions.assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `플레이어 태그 순서는 달라도 된다`() {
        // given

        // when
        val result1 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()
        val result2 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player2")
                .addPlayerTag("player1")
                .addPlayerTag("player3")
                .build()

        // then
        Assertions.assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun `날짜가 다르면 다른 키가 생성된다`() {
        // given

        // when
        val result1 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()
        val result2 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 2, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()

        // then
        Assertions.assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `플레이어 수가 다르면 다른 키가 생성된다`() {
        // given

        // when
        val result1 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()
        val result2 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .build()

        // then
        Assertions.assertThat(result1).isNotEqualTo(result2)
    }

    @Test
    fun `플레이어가 다르면 다른 키가 생성된다`() {
        // given

        // when
        val result1 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player1")
                .addPlayerTag("player2")
                .addPlayerTag("player3")
                .build()
        val result2 =
            BattleKeyBuilder(
                ZonedDateTime.of(LocalDateTime.of(2021, 1, 1, 0, 0), ZoneId.systemDefault()).toOffsetDateTime(),
            ).addPlayerTag("player4")
                .addPlayerTag("player5")
                .addPlayerTag("player6")
                .build()

        // then
        Assertions.assertThat(result1).isNotEqualTo(result2)
    }
}
