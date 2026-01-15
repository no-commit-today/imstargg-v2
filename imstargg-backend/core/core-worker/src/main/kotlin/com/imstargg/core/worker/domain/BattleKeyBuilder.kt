package com.imstargg.core.worker.domain

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.OffsetDateTime

class BattleKeyBuilder(
    private val battleTime: OffsetDateTime,
) {
    private val playerTags = mutableListOf<String>()

    fun addPlayerTag(playerTag: String): BattleKeyBuilder {
        playerTags.add(playerTag)
        return this
    }

    fun build(): String {
        val keySeed = createKeySeed()
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(keySeed)
        return bytesToHex(hash)
    }

    private fun createKeySeed(): ByteArray {
        val joinedPlayerTagBytes =
            playerTags
                .sorted()
                .joinToString("")
                .toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(8 + joinedPlayerTagBytes.size)
        buffer.putLong(battleTime.toEpochSecond())
        buffer.put(joinedPlayerTagBytes)
        return buffer.array()
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (b in hash) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}