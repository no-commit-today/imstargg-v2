package com.imstargg.core.application.operation

import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class IpHasher(
    private val properties: IpHasherProperties,
) {
    init {
        require(properties.ipSalt.isNotBlank()) { "app.operation.feedback.ip-salt must not be blank" }
    }

    fun hash(ip: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((properties.ipSalt + ip).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
