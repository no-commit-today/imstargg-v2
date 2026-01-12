package com.imstargg.client.brawlstars.response

data class ListResponse<T>(
    val items: List<T>,
    val paging: PagingResponse?,
)
