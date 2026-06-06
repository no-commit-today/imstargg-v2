package com.imstargg.infrastructure.db.core

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

fun Instant.toKoreaLocalDate(): LocalDate = OffsetDateTime.ofInstant(this, KOREA_ZONE).toLocalDate()
