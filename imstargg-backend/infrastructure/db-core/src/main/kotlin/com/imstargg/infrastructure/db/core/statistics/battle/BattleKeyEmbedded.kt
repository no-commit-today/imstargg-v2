package com.imstargg.infrastructure.db.core.statistics.battle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant

/**
 * 배틀 식별자 값 객체. SHA-256(epochSeconds + sorted tags).
 *
 * 호출 측이 집계 의도에 따라 태그 범위를 결정한다:
 * - 결과형 전투: 전체 플레이어 태그 → 같은 배틀이면 수집 주체와 무관하게 같은 키.
 * - 순위형 전투: 수집 주체의 자기 팀 태그만 → 같은 팀이면 수집 주체와 무관하게 같은 키.
 */
@JvmInline
value class BattleKeyEmbedded(
    val value: String,
) {
    companion object {
        fun create(
            battleTime: Instant,
            playerTags: Collection<String>,
        ): BattleKeyEmbedded {
            val joined =
                playerTags
                    .sorted()
                    .joinToString("")
                    .toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + joined.size)
            buffer.putLong(battleTime.epochSecond)
            buffer.put(joined)
            val digest = MessageDigest.getInstance("SHA-256").digest(buffer.array())
            return BattleKeyEmbedded(digest.toHex())
        }

        private fun ByteArray.toHex(): String =
            joinToString("") { byte ->
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) "0$hex" else hex
            }
    }
}
