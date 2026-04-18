package com.imstargg.core.admin.application

import java.io.InputStream

data class ImageUpdate(
    val inputStream: InputStream,
    val contentType: String,
    val ext: String,
)
