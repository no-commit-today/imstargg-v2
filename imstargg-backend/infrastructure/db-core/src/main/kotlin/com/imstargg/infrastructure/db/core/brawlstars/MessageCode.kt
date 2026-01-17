package com.imstargg.infrastructure.db.core.brawlstars

import jakarta.persistence.Embeddable

/**
 * 메시지 코드 생성을 위한 값 객체
 * 여러 인자를 구분자(:)로 연결하여 lowercase 문자열로 변환
 */
@Embeddable
data class MessageCode(
    val value: String,
) {
    companion object {
        private const val SEPARATOR = ":"

        fun of(vararg parts: Any): MessageCode {
            val value = parts.joinToString(SEPARATOR) { it.toString().lowercase() }
            return MessageCode(value)
        }
    }
}
