package com.imstargg.core.enums

enum class RenewalStatus {
    REQUESTED,
    PENDING,
    EXECUTING,
    COMPLETED,
    NOT_FOUND,
    FAILED,
    ;

    fun isCompleted(): Boolean = this == COMPLETED || this == NOT_FOUND || this == FAILED

    fun isProgressing(): Boolean = !isCompleted()
}
