package com.imstargg.core.batch.support.alert

class AlertCommandBuilder {
    private var title: String? = null
    private var alertType: AlertType? = null
    private var content: String? = null
    private var ex: Exception? = null

    fun warn(): AlertCommandBuilder {
        this.alertType = AlertType.WARN
        return this
    }

    fun error(): AlertCommandBuilder {
        this.alertType = AlertType.ERROR
        return this
    }

    fun title(title: String): AlertCommandBuilder {
        this.title = title
        return this
    }

    fun content(content: String): AlertCommandBuilder {
        this.content = content
        return this
    }

    fun ex(ex: Exception): AlertCommandBuilder {
        this.ex = ex
        return this
    }

    fun build(): AlertCommand {
        var buildTitle = title ?: throw IllegalArgumentException("title 은 필수입니다.")
        if (alertType != null) {
            buildTitle = "${alertType!!.emoji} $buildTitle"
        }
        return AlertCommand(buildTitle, content, ex)
    }

    private enum class AlertType(
        val emoji: String,
    ) {
        WARN("⚠️"),
        ERROR("🚨"),
    }
}
