package com.imstargg.infrastructure.db.core.lock

interface LockTemplate {
    fun <T> execute(
        lockName: LockName,
        timeoutSeconds: Int,
        supplier: () -> T,
    ): T
}
