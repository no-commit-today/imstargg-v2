package com.imstargg.core.batch.support.alert

import java.io.PrintWriter
import java.io.StringWriter

data class AlertCommand(
    val title: String,
    val content: String?,
    val ex: Exception?,
) {
    fun exStackTrace(): String? {
        if (ex == null) {
            return null
        }
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        ex.printStackTrace(pw)
        return sw.toString()
    }

    companion object {
        fun builder() = AlertCommandBuilder()
    }
}
