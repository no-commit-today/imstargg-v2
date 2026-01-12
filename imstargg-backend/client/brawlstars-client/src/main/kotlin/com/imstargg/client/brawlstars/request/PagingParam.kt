package com.imstargg.client.brawlstars.request

data class PagingParam(
    val before: String? = null,
    val after: String? = null,
    val limit: Int? = null,
) {
    companion object {
        val DEFAULT = PagingParam()
    }
}
