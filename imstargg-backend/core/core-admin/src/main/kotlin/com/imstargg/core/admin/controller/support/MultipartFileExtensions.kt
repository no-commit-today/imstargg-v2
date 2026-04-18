package com.imstargg.core.admin.controller.support

import com.imstargg.core.admin.application.ImageUpdate
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

fun MultipartFile.toImageUpdate(): ImageUpdate {
    val ext =
        originalFilename
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 확장자가 존재해야 합니다.")
    val contentType =
        contentType
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 contentType 이 존재해야 합니다.")
    return ImageUpdate(
        inputStream = inputStream,
        contentType = contentType,
        ext = ext,
    )
}
