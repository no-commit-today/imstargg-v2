package com.imstargg.core.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val message: String,
    val logLevel: LogLevel,
) {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다.", LogLevel.ERROR),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "잘못된 값이 입력되었습니다.", LogLevel.DEBUG),
}
