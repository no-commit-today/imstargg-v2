package com.imstargg.core.admin.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

private val log = KotlinLogging.logger("com.imstargg.core.admin.application.ImageStorageCleanup")

fun ImageStorageService.deleteAfterCommit(key: String) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runCatching { delete(key) }
                        .onFailure { log.warn(it) { "Failed to delete orphaned image: $key" } }
                }
            },
        )
    } else {
        runCatching { delete(key) }
            .onFailure { log.warn(it) { "Failed to delete orphaned image: $key" } }
    }
}
