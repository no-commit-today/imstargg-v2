package com.imstargg.core.admin.application

import java.io.InputStream

interface ImageStorageService {
    fun upload(
        key: String,
        inputStream: InputStream,
        contentType: String,
    )

    fun delete(key: String)
}
