package com.imstargg.infrastructure.db.core.statistics.battle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.OffsetDateTime

@JvmInline
value class BattleKeyEmbedded(val value: String) {
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
