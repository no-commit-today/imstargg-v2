package com.imstargg.core.batch.support.alert

import org.slf4j.LoggerFactory

internal class NoOpsAlertManager : AlertManager {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun alert(alertCommand: AlertCommand) {
        log.info("[알람 호출]:\n{}", alertCommand)
    }
}
