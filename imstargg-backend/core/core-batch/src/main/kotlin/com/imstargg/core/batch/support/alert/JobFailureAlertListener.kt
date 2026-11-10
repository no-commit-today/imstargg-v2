package com.imstargg.core.batch.support.alert

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.boot.batch.autoconfigure.JobExecutionEvent
import org.springframework.boot.context.event.ApplicationFailedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.NestedExceptionUtils
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 잡이 실패하면 알린다. 잡마다 리스너를 달지 않아도 core-batch 의 모든 잡에 걸린다.
 *
 * 실패는 두 경로로 온다.
 * - 잡이 시작된 뒤의 실패: Boot 의 잡 러너가 실행을 마친 뒤 `JobExecutionEvent` 를 낸다. 상태가 `COMPLETED` 가 아니면 알린다.
 * - 잡이 시작되기 전의 실패(필수 파라미터 누락 등): 러너가 예외를 던져 `JobExecutionEvent` 가 나오지 않는다.
 *   앱 실패로 끝나므로 `ApplicationFailedEvent` 로 받는다. 이때는 잡 실행이 없어 잡 이름은 설정(`spring.batch.job.name`)에서,
 *   파라미터는 실행 인자에서 읽는다.
 *
 * 컨텍스트가 뜨기 전의 실패(DB 연결 실패로 기동이 멈춘 경우 등)는 이 빈이 없어 알리지 못한다.
 * 이 장치가 생기기 전에는 실패를 알리는 곳이 없어 운영 잡이 며칠씩 실패해도 몰랐다(2026-11).
 */
@Component
class JobFailureAlertListener(
    private val alertManager: AlertManager,
    private val environment: Environment,
) {
    @EventListener
    fun onJobExecution(event: JobExecutionEvent) =
        alertSafely {
            val execution = event.jobExecution
            if (execution.status == BatchStatus.COMPLETED) return@alertSafely null

            val failure = execution.allFailureExceptions.firstOrNull()
            failureAlert(
                jobName = execution.jobInstance.jobName,
                details =
                    listOfNotNull(
                        "파라미터: ${execution.jobParameters.parameters().joinToString { "${it.name()}=${it.value()}" }}",
                        "상태: ${execution.status}, 종료 코드: ${execution.exitStatus.exitCode}",
                        failure?.let { "예외: ${it.summary()}" },
                    ),
                ex = failure as? Exception,
            )
        }

    @EventListener
    fun onApplicationFailed(event: ApplicationFailedEvent) =
        alertSafely {
            val exception = event.exception
            failureAlert(
                jobName = environment.getProperty(JOB_NAME_PROPERTY) ?: UNKNOWN,
                details =
                    listOf(
                        "실행 인자: ${event.args.joinToString(" ")}",
                        "잡이 시작되기 전에 앱이 실패했다.",
                        "예외: ${NestedExceptionUtils.getMostSpecificCause(exception).summary()}",
                    ),
                ex = exception as? Exception,
            )
        }

    /**
     * 알림 때문에 잡의 종료 코드가 바뀌지 않게, 알림 본문을 만들고 보내는 중의 예외는 로그만 남긴다.
     * 리스너의 예외는 Boot 러너로 번져 실패한 잡을 앱 실패로 바꾼다.
     */
    private fun alertSafely(compose: () -> AlertCommand?) {
        try {
            compose()?.let { alertManager.alert(it) }
        } catch (alertException: Exception) {
            log.error("잡 실패 알림 전송 실패", alertException)
        }
    }

    private fun failureAlert(
        jobName: String,
        details: List<String>,
        ex: Exception?,
    ): AlertCommand {
        val builder =
            AlertCommand
                .builder()
                .error()
                .title("[$jobName] 실패")
                .content((details + "파드: ${environment.getProperty(HOSTNAME_PROPERTY) ?: UNKNOWN}").joinToString("\n"))
        ex?.let { builder.ex(it) }
        return builder.build()
    }

    private fun Throwable.summary(): String = "${javaClass.name}: ${message?.lineSequence()?.first().orEmpty()}"

    companion object {
        private const val JOB_NAME_PROPERTY = "spring.batch.job.name"
        private const val HOSTNAME_PROPERTY = "HOSTNAME"
        private const val UNKNOWN = "알 수 없음"
        private val log = LoggerFactory.getLogger(JobFailureAlertListener::class.java)
    }
}
