package com.imstargg.infrastructure.redis.core.statistics.player

import com.imstargg.core.enums.BattleType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.HashOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.LocalDate

class PlayerBrawlerStatsCacheStoreTest {
    private val redisTemplate = mockk<StringRedisTemplate>()
    private val hashOps = mockk<HashOperations<String, String, String>>()
    private val codec = PlayerBrawlerStatsCacheCodec()
    private val sut = PlayerBrawlerStatsCacheStore(redisTemplate, codec)

    private val date = LocalDate.of(2026, 5, 1)
    private val value =
        PlayerBrawlerStatsCacheValue(
            endBattleDate = "2026-05-31",
            items = listOf(PlayerBrawlerStatsCacheValue.Item(16000000L, 5L, 1L, 0L)),
        )

    @Test
    fun `find 는 키와 필드로 조회해 디코딩한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every {
            hashOps.get("playerBrawlerStats:v1:#TAG", "RANKED:2026-05-01")
        } returns codec.encode(value)

        // when
        val result = sut.find("#TAG", BattleType.RANKED, date)

        // then
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `find 결과가 없으면 null 을 반환한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.get(any(), any()) } returns null

        // when & then
        assertThat(sut.find("#TAG", BattleType.RANKED, date)).isNull()
    }

    @Test
    fun `find 중 예외가 나면 null 을 반환한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } throws RuntimeException("redis down")

        // when & then
        assertThat(sut.find("#TAG", BattleType.RANKED, date)).isNull()
    }

    @Test
    fun `put 은 직렬화해 저장하고 TTL 을 설정한다`() {
        // given
        every { redisTemplate.opsForHash<String, String>() } returns hashOps
        every { hashOps.put(any(), any(), any()) } returns Unit
        every { redisTemplate.expire(any<String>(), any<Duration>()) } returns true

        // when
        sut.put("#TAG", BattleType.RANKED, date, value)

        // then
        verify { hashOps.put("playerBrawlerStats:v1:#TAG", "RANKED:2026-05-01", codec.encode(value)) }
        verify { redisTemplate.expire("playerBrawlerStats:v1:#TAG", Duration.ofHours(1)) }
    }

    @Test
    fun `evict 는 플레이어 키를 삭제한다`() {
        // given
        every { redisTemplate.delete("playerBrawlerStats:v1:#TAG") } returns true

        // when
        sut.evict("#TAG")

        // then
        verify { redisTemplate.delete("playerBrawlerStats:v1:#TAG") }
    }

    @Test
    fun `evict 중 예외가 나도 전파하지 않는다`() {
        // given
        every { redisTemplate.delete(any<String>()) } throws RuntimeException("redis down")

        // when & then (예외 미전파)
        sut.evict("#TAG")
    }
}
