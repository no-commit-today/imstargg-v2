package com.imstargg.infrastructure.db.core.statistics.battle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.OffsetDateTime

/**
 * 배틀 식별자 값 객체. SHA-256(epochSeconds + sorted tags).
 *
 * [forResult] 와 [forRank] 는 입력이 같으면 **같은 해시를 반환**한다.
 * 팩토리 분리는 호출 측 의도를 드러내기 위한 것이며(결과형 전투: 전체 플레이어 태그,
 * 순위형 전투: 자기 팀 플레이어 태그), 내부 해시 함수는 공유한다.
 */
@JvmInline
value class BattleKeyEmbedded(
    val value: String,
) {
    companion object {
        fun forResult(
            battleTime: OffsetDateTime,
            playerTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTags)

        fun forRank(
            battleTime: OffsetDateTime,
            playerTeamTags: Collection<String>,
        ): BattleKeyEmbedded = hash(battleTime, playerTeamTags)

        private fun hash(
            battleTime: OffsetDateTime,
            tags: Collection<String>,
        ): BattleKeyEmbedded {
            val joined =
                tags
                    .sorted()
                    .joinToString("")
                    .toByteArray(Charsets.UTF_8)
            val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + joined.size)
            buffer.putLong(battleTime.toEpochSecond())
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
