package com.imstargg.core.error

class CoreException : RuntimeException {
    val errorType: ErrorType

    companion object {
        private fun errorMessage(
            errorType: ErrorType,
            message: String? = null,
        ): String = "[$errorType]:${errorType.message}${message?.let { " >>> $it" } ?: ""}"
    }

    constructor(errorType: ErrorType) : super(errorMessage(errorType)) {
        this.errorType = errorType
    }

    constructor(message: String) : this(ErrorType.DEFAULT_ERROR, message)

    constructor(errorType: ErrorType, message: String) : super(errorMessage(errorType, message)) {
        this.errorType = errorType
    }

    constructor(message: String, cause: Throwable) : this(ErrorType.DEFAULT_ERROR, message, cause)

    constructor(errorType: ErrorType, message: String, cause: Throwable) :
        super(errorMessage(errorType, message), cause) {
        this.errorType = errorType
    }
}
