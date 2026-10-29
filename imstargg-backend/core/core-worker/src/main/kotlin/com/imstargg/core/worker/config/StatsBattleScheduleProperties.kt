package com.imstargg.core.worker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * @property interval 틱 주기(fixed-rate). 직전 틱 종료 후의 대기 시간이 아니라 틱 시작 간격이다.
 * 틱이 주기보다 오래 걸리면 다음 틱이 곧바로 이어 실행되며, 그 몰림은 brawlstars 공용 레이트리미터가 막는다.
 * @property playerCount 한 틱에 동시에 수집하는 플레이어 수. 초당 API 호출 수는 playerCount / interval 이다.
 */
@ConfigurationProperties(prefix = "app.schedule.stats-battle")
data class StatsBattleScheduleProperties(
    val enabled: Boolean = false,
    val interval: Duration = Duration.ofSeconds(60),
    val playerCount: Int = 10,
)
