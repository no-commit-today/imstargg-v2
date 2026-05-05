package com.imstargg.core.api.controller.v1.response

data class PlayerBrawlerItemResponse<T>(
    val item: T,
    val owned: Boolean,
)
