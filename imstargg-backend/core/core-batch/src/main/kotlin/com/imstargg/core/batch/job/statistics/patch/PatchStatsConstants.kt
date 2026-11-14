package com.imstargg.core.batch.job.statistics.patch

import java.time.Duration

object PatchStatsConstants {
    /** 전투를 이만큼 읽을 때마다 저장한다. 초기값이고, 가동 뒤 재 보고 고친다. */
    const val CHECKPOINT_BATTLE_COUNT = 100_000L

    /** 상한 id 는 실행 시작에서 이만큼 앞선 때까지 저장된 원본에서 잡는다. 저장 중인 트랜잭션이 끝나기를 기다리는 여유다. */
    val BATTLE_ID_CAP_MARGIN: Duration = Duration.ofMinutes(10)

    /** 상한 id 를 찾는 전투 날짜의 폭. 적재가 받는 가장 오래된 전투(7일 전)에 하루를 더했다. */
    const val BATTLE_ID_CAP_BATTLE_DAYS = 8L
}
