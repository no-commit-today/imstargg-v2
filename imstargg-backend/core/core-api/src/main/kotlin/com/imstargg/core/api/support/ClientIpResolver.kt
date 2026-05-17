package com.imstargg.core.api.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class ClientIpResolver {
    fun resolve(request: HttpServletRequest): String {
        val forwarded = request.getHeader(HEADER_FORWARDED_FOR)
        if (!forwarded.isNullOrBlank()) {
            val firstIp = forwarded.substringBefore(',').trim()
            if (firstIp.isNotEmpty()) {
                return firstIp
            }
        }
        return request.remoteAddr
    }

    companion object {
        private const val HEADER_FORWARDED_FOR = "X-Forwarded-For"
    }
}
