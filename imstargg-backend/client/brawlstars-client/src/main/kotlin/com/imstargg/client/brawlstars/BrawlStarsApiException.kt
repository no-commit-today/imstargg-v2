package com.imstargg.client.brawlstars

import feign.FeignException

sealed class BrawlStarsApiException(
    message: String,
    cause: FeignException,
) : RuntimeException(message, cause) {
    val status: Int get() = feignException.status()

    val feignException: FeignException get() = cause as FeignException

    companion object {
        fun from(
            message: String,
            ex: FeignException,
        ): BrawlStarsApiException = when {
            isClientError(ex.status()) -> BrawlStarsApiClientException.from(message, ex)
            isServerError(ex.status()) -> BrawlStarsApiServerException.from(message, ex)
            else -> BrawlStarsApiClientException(message, ex)
        }

        private fun isClientError(status: Int): Boolean = status in 400..499

        private fun isServerError(status: Int): Boolean = status in 500..599
    }
}

open class BrawlStarsApiClientException(
    message: String,
    cause: FeignException,
) : BrawlStarsApiException(message, cause) {
    class NotFound(
        message: String,
        cause: FeignException,
    ) : BrawlStarsApiClientException(message, cause)

    class TooManyRequests(
        message: String,
        cause: FeignException,
    ) : BrawlStarsApiClientException(message, cause)

    companion object {
        fun from(
            message: String,
            ex: FeignException,
        ): BrawlStarsApiClientException = when (ex.status()) {
            404 -> NotFound(message, ex)
            429 -> TooManyRequests(message, ex)
            else -> BrawlStarsApiClientException(message, ex)
        }
    }
}

open class BrawlStarsApiServerException(
    message: String,
    cause: FeignException,
) : BrawlStarsApiException(message, cause) {
    class ServiceUnavailable(
        message: String,
        cause: FeignException,
    ) : BrawlStarsApiServerException(message, cause)

    companion object {
        fun from(
            message: String,
            ex: FeignException,
        ): BrawlStarsApiServerException = when (ex.status()) {
            503 -> ServiceUnavailable(message, ex)
            else -> BrawlStarsApiServerException(message, ex)
        }
    }
}
