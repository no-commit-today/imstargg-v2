package com.imstargg.infrastructure.db.core

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * 서비스의 하루를 자르는 시간대. 시각은 `Instant`(timestamptz)로 저장하고, 날짜로 바꿀 때만 이 시간대를 쓴다.
 *
 * 전투의 `battle_date`(파티션 키·일 배치 단위)와 앱 `Clock` 빈(API 의 "이번 주/이번 달")이 모두 이 값을 따라야
 * 두 날짜 경계가 맞는다. 각 앱의 `ClockConfig` 는 이 상수를 참조하고 시간대를 따로 적지 않는다.
 * 배치 CronJob 이 넘기는 `date` 파라미터(imstargg-deploy 의 core-batch values.yaml 셸 계산식)도 같은 규칙을 따른다.
 *
 * UTC 로 두지 않는 이유: KST 하루는 KST 00시에 닫혀 이용자가 적은 02~09시에 전일 집계를 돌릴 수 있고,
 * 한국은 서머타임이 없어 오프셋이 +9 로 고정된다.
 */
val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

fun Instant.toKoreaLocalDate(): LocalDate = OffsetDateTime.ofInstant(this, KOREA_ZONE).toLocalDate()
