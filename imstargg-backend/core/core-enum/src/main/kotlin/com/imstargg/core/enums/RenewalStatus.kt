package com.imstargg.core.enums

enum class RenewalStatus {
    REQUESTED,
    PENDING,
    EXECUTING,
    COMPLETED,
    NOT_FOUND,
    FAILED,
    ;

    fun isProgressing(): Boolean = this == REQUESTED || this == PENDING || this == EXECUTING
}
