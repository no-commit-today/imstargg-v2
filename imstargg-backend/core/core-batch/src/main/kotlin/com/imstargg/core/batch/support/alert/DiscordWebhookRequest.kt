package com.imstargg.core.batch.support.alert

class DiscordWebhookRequest(
    content: String,
) {
    val content: String =
        if (content.length > MAX_CONTENT_LENGTH) {
            content.substring(0, MAX_CONTENT_LENGTH - POSTFIX.length) + POSTFIX
        } else {
            content
        }

    companion object {
        private const val MAX_CONTENT_LENGTH = 2000
        private const val POSTFIX = "\n...자세한 내용은 로그를 참고..."

        fun from(alertCommand: AlertCommand): DiscordWebhookRequest {
            val sb = StringBuilder()

            sb.append("# ").append(alertCommand.title).append("\n\n")

            if (alertCommand.content != null) {
                sb.append("[내용]\n").append(alertCommand.content).append("\n\n")
            }

            if (alertCommand.ex != null) {
                sb.append("[예외]\n").append(alertCommand.exStackTrace()).append("\n\n")
            }

            return DiscordWebhookRequest(sb.toString())
        }
    }
}
