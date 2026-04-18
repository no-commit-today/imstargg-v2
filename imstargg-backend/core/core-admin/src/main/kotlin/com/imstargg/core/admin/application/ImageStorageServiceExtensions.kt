package com.imstargg.core.admin.application

import org.slf4j.LoggerFactory
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

private val log = LoggerFactory.getLogger("com.imstargg.core.admin.application.ImageStorageCleanup")

fun ImageStorageService.deleteAfterCommit(key: String) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runCatching { delete(key) }
                        .onFailure { log.warn("Failed to delete orphaned image: {}", key, it) }
                }
            },
        )
    } else {
        runCatching { delete(key) }
            .onFailure { log.warn("Failed to delete orphaned image: {}", key, it) }
    }
}
