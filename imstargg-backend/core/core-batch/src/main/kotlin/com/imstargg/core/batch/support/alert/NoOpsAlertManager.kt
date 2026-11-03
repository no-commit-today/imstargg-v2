package com.imstargg.core.batch.support.alert

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal class NoOpsAlertManager : AlertManager {
    override fun alert(alertCommand: AlertCommand) {
        log.info { "[알람 호출]:\n$alertCommand" }
    }
}
