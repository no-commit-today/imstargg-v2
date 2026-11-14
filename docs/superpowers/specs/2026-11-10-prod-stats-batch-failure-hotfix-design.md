# 운영 통계 배치 실패 대응 설계 (hotfix)

- 작성일: 2026-11-10
- 대상: 운영 main 의 `core/core-batch`(DB 소켓 설정, 순위형 1인 집계기, 이벤트 동기화 잡, 잡 실패 알림). 운영 DB 복구 실행은 런북으로 적는다.
- 상태: 안 H 로 정해 main 기준으로 H1 ~ H4 를 구현한 뒤, 사용자 결정으로 develop 에 먼저 PR 을 올린다(2026-11-10, 브랜치 `fix/stats-batch-failures`). develop 에서는 H2 를 §4.6 의 형태(`StatsEventMode` 순위형 판정)로 넣고, H4 의 Writer 두 줄은 develop 에 이미 있어 incrementer 삭제만 넣는다. main 기준 구현은 로컬 브랜치 `hotfix/stats-batch-failures` 에 커밋만 해 두었다(푸시하지 않음). 운영 반영 경로(main hotfix 를 따로 낼지, develop 릴리즈로 낼지)는 아직 정하지 않았다
- 관련: 스펙 D(늦게 들어온 전투의 재집계, `2026-11-10-stats-late-battle-reaggregation-design.md`, 검토 중이라 아직 저장소에 없다)를 조사하다 찾았다. 스펙 D 의 결정 5(순위형 모드 거름)와 같은 결함을 운영 main 에서 먼저 고친다. 스펙 B·C1 의 배포 런북과는 §5 에서 맞춘다.
- 함께 볼 것: 같은 날 PR #85(`hotfix/extend-stats-raw-partition-retention`)가 원본 보관을 10일 → 45일로 늘리고 core-batch 를 0.4.5 로 올렸다(2026-11-10 main 병합, 운영 배포 여부는 이 문서에서 확인하지 않았다). 이 문서의 버전(§4)과 복구 마감(§4.5, §8)은 #85 의 배포 시점에 달렸다. develop 에서는 core-batch 가 이미 0.5.0-SNAPSHOT 이라 이 수정으로 버전이 바뀌지 않는다.

## 1. 상황 (2026-11-10 09시 KST 기준)

운영 main(`96787b06`, core-batch 이미지 `0.4.3-7aef2d1`)의 배치 잡 네 개가 날마다 실패한다. 각 잡은 쿠버네티스 Job 기본 재시도로 6번씩 시도하고, 6번 모두 실패한다. core-batch 모든 잡의 완료 줄과 예외를 지난 7 ~ 30일 VictoriaLogs 로 모아 확인했다.

| 잡 | 실패 시작 | 운영에 남은 마지막 값 | 예외 |
|---|---|---|---|
| `brawlerBattleRankStatsJob` (02:00) | date 11-05 | `brawler_battle_rank_stats` 는 battle_date 11-04 까지 | `IllegalArgumentException: rank(19)는 최대 10 이어야 합니다.` |
| `brawlerEnemyBattleResultStatsJob` (04:00) | date 11-07 | 묶음 [11-02 ~ 11-08] 은 11-07 04:26 기록(date 11-06 실행분), [11-09 ~ 11-15] 는 없음 | `SocketTimeoutException: Read timed out` |
| `brawlerPairBattleResultStatsJob` (07:00) | date 11-07 | 묶음 [11-02 ~ 11-08] 은 11-07 07:11 기록, [11-09 ~ 11-15] 는 없음 | 같음 |
| `battleEventSyncJob` (08:00) | 로그가 남은 10-11 부터 모든 실행(209회). 2026-04-19 이후로 추정(§2.3) | `battle_event_solo_rank_event` 가 **0행**이다. 경쟁전 이벤트 API(`BattleEventService.getSoloRankEvents`)가 빈 목록을 돌려준다 | `InvalidJobParametersException: The JobParameters do not contain required keys: [date]` (잡 시작 전) |

- 그 밖의 실패는 이 문서에서 다루지 않는다. `rankingCollectJob`(30분마다, 7일 374회 중 47회 실패)과 `battleEventRotationSyncJob`(매시, 173회 중 6회)은 대부분 Brawl Stars API 의 429·503·HTTP 시간 초과다. 다음 실행이 독립적으로 다시 수집한다.
- 묶음 기록 시각은 전투가 가장 많은 이벤트(15000118)의 행으로 확인했다(이벤트로 시작하는 유니크 인덱스를 타는 조회). 운영 main 의 조합 Writer 는 이벤트마다 따로 쓰므로, 실패한 실행이 일부 이벤트만 새로 썼을 수 있다.
- 1인 결과형(`brawlerBattleResultStatsJob`)은 매일 성공한다.
- **실패를 알리는 장치가 없다.** core-batch 에 잡 실패 알림이 없고, 쿠버네티스 Job 지표(`kube_job_*`)도 VictoriaMetrics 에 수집되지 않는다. 그래서 며칠 동안 아무도 몰랐다.

## 2. 원인 (확정)

### 2.1 순위형 1인 — 모드 75 의 순위

- 11-05 부터 20인 솔로 쇼다운(모드 75)이 원본에 들어온다. 순위가 1 ~ 20 이다. 11-09 원본에는 순위 10 을 넘는 행이 2,450건 있다.
- 운영 main 의 `BrawlerBattleRankStatsAggregator` 는 모드를 거르지 않는다. 쓰기 단계의 `RankCountsEmbedded.fromMap` 이 `require(maxRank <= MAX_RANK(10))` 에서 예외를 던져 그 날짜 전체가 실패한다.
- 데이터 때문에 나는 결정적인 오류라 재시도해도 같다.
- develop 의 순위형 집계기도 모드를 거르지 않는다. 그래서 스펙 A·B·C1 을 그대로 배포해도 같은 오류가 난다(스펙 D 결정 5).

### 2.2 카운터·2인 — 3초 소켓 제한과 느린 조회

**(a) core-batch 의 DB 소켓 읽기 제한이 3초다.**

- db-core 의 `application-db.yml` 은 `socketTimeout: 3` 이다. core-batch 의 `application.yml` 은 `on-profile: prod` 문서에서 `socketTimeout: 0`(제한 없음)으로 덮으려 한다(2026-04-19 `335eed50` 의 의도).
- 그런데 프로필 전용 파일(`application-db.yml`)은 `application.yml` 안의 프로필 문서보다 우선한다. 그래서 운영 core-batch 의 값은 **3초**다.
- **확인 방법**: 운영 jar 의 classpath 루트처럼 설정 파일 다섯 개(core-batch `application.yml`, `application-db.yml`, `logging.yml`, `http-client.yml`, `brawlstars-client.yml`)를 한 폴더에 두고 `--spring.profiles.active=prod` 로 띄우는 임시 테스트를 돌렸다(저장소에 남기지 않았다). 활성 프로필은 `[prod, db]` 이고, `storage.datasource.core.data-source-properties.socketTimeout` 은 `3` 이었다.
- 같은 결함의 다른 흔적: 11-08 `partitionManageJob` 이 한 번 3.15초 만에 실패했다가 재시도로 성공했다.
- core-api·core-worker 는 이 키를 덮지 않으므로 영향이 없다.

**(b) 운영 main 의 조합 Reader 가 느리다.**

- 2인·카운터 Reader 는 이벤트마다 `battle_date between 묶음 and event_brawlstars_id = X and id > lastId order by id limit 5000` 을 페이지마다 다시 낸다.
- 전투가 많은 이벤트는 파티션마다 기본키 인덱스를 id 순으로 훑으면서 이벤트로 거르는 Merge Append 계획을 탄다(`explain` 으로 확인). 그래서 페이지 하나가 수만 ~ 수십만 행의 heap 을 읽는다. 전투가 적은 이벤트는 `(battle_date, event)` 인덱스를 써서 빠르다.
- 11-03 18시부터 수집량이 약 3배가 됐고, 주 묶음이 찰수록 읽을 행이 는다. 잡 소요 시간이 4분 35초(date 11-03) → 21분 42초(date 11-06)로 늘었다. 11-07 부터는 페이지 쿼리 하나가 3초를 넘어 (a) 에 걸렸다. 실패 위치는 `BrawlerPairBattleResultStatsReader.read`(`.fetch()`)다.

**develop 에서는**: 스펙 B 의 조합 Reader 는 이벤트 없이 묶음을 keyset 으로 한 번 읽어 (b) 가 없어진다. (a) 는 develop 에도 그대로 있다. 묶음 통째 삭제나 큰 쿼리가 3초를 넘으면 같은 식으로 실패한다.

### 2.3 이벤트 동기화 — incrementer 가 `date` 를 버린다

- `battleEventSyncJob` 은 `date` 를 필수로 검증하면서 `RunIdIncrementer` 도 단다. incrementer 는 2026-04-19 `6ff59a08` 에서 다시 들어갔다(`3c102ec5` 가 4-07 에 지웠던 것).
- Spring Batch 6 의 `SimpleJobOperator.start(job, params)` 는 잡에 incrementer 가 있으면 넘긴 파라미터를 버리고 `startNextInstance` 로 새 파라미터(`run.id`)를 만든다. 소스의 경고문은 "Additional parameters will be ignored." 다(spring-batch-core 6.0.2). 그래서 CronJob 이 넘긴 `date` 가 사라지고, 검증기가 잡 시작 전에 예외를 던진다.
- 같은 저장소의 `RankingCollectJobConfig` KDoc 이 이 동작을 이미 적어 두었다. 다른 incrementer 잡(`brawlerSyncJob` 등 다섯 개)은 필수 파라미터가 없어 영향이 없다. `statsPlayerSyncJob` 은 incrementer 가 없다(같은 파일의 `statsPlayerSyncFromRankingJob` 만 단다).
- **영향**: 경쟁전 이벤트 목록이 비어 있고, 통계 전투에 나온 미등록 이벤트·불일치 경고가 나가지 않는다. 최근 9일 통계에 나온 이벤트 101개 중 44개가 `battle_event` 에 없다. develop 의 같은 잡도 incrementer 가 그대로라, 스펙 A 가 기대한 "새 맵은 이 잡의 경고를 보고 등록한다" 도 동작하지 않는다.

## 3. 대응안 비교

| | H. main hotfix 먼저 (권함) | D. develop(스펙 A·B·C1) 앞당겨 배포 |
|---|---|---|
| 바꾸는 것 | core-batch 만. 소켓 설정(H1), 순위형 집계기 거름(H2), 잡 실패 알림(H3), 이벤트 동기화 잡의 incrementer 삭제와 경고 중복 수정(H4) | develop 전체. 단 H1·H2·H4 의 incrementer 삭제를 develop 에 먼저 넣어야 한다 |
| 조합·카운터 | 3초 제한을 풀면 예전처럼 끝난다(11-06 까지 21분 안). 묶음이 다 차면 30 ~ 60분으로 추정 | 새 Reader 라 빠르다. 대신 스펙 B 이관(`V261104a`)과 C1 런북을 함께 해야 한다 |
| 복구 기한 | 묶음 [11-02 ~ 11-08] 을 11-13 02:00 전에 다시 써야 한다(§4.5) | 같은 기한이 스펙 B·C1 배포에도 걸린다 |
| 위험 | 작다. 운영 main 의 동작은 그대로이고 실패 원인만 없앤다 | 크다. 스키마 이관, 봇 제외, 런북 여러 단계를 이틀 안에 해야 한다 |

H 를 권한다. 기한 안에 확실히 복구하고, 같은 수정을 develop 으로 backmerge 해서 A·B·C1 릴리즈도 같은 문제를 피한다.

## 4. 설계 — 안 H

`main` 에서 `hotfix/stats-batch-failures` 브랜치를 따서 고친다. core-batch 버전은 `/release` 의 hotfix 흐름으로 정한다. PR #85 가 이미 0.4.4 → 0.4.5 를 쓰므로, #85 가 먼저 병합되면 이 hotfix 는 0.4.6 이다. 두 hotfix 를 한 릴리즈로 묶으면 0.4.5 하나다(어느 쪽인지는 사용자 결정). 두 브랜치가 함께 바꾸는 소스 파일은 없다. main 에는 배포되지 않은 core-batch 0.4.4 가 있다. 배포 이미지(`7aef2d1`) 이후 core-batch 가 싣는 모듈(core-batch, core-enum, support, db-core, brawlstars-client)과 빌드 설정의 차이는 `gradle.properties` 의 버전, brawlstars-client README, db-core 의 `StatsPlayerJpaRepository` 한 파일(워커와 함께 이미 운영 중인 코드)뿐이다.

### 4.1 H1. core-batch 의 소켓 제한을 바로잡는다

- core-batch `application.yml` 의 그룹을 `local`/`local-dev`/`dev`/`prod` 모두 `[db, batch-db]` 로 바꾼다. core-batch 리소스에 `application-batch-db.yml` 을 새로 두고 `storage.datasource.core.data-source-properties.socketTimeout` 을 넣는다.
- 그룹의 뒤쪽 프로필이 앞쪽 프로필 파일보다 우선한다. 같은 임시 테스트로 `[prod, db, batch-db]` 에서 이 값이 이기는 것을 확인했다.
- `application.yml` 의 네 프로필 문서에 있는 `socketTimeout: 0` 은 지운다. 효과가 없는 줄이다. `minimum-idle`·`maximum-pool-size` 는 db-core 가 설정하지 않아 지금도 적용되므로 그대로 둔다.
- **값**: 구현은 권하는 값 **600초**로 했다. 원래 의도대로 `0`(제한 없음)으로 하려면 `application-batch-db.yml` 한 줄과 가드 테스트의 기대값만 바꾼다. 600초를 권하는 이유: 원래 의도는 `0`(제한 없음)이다. 0 이면 쿼리 하나가 멈췄을 때 잡이 끝나지 않는다. CronJob 이 `concurrencyPolicy: Forbid` 라서 다음 날 실행까지 막힌다. 600초는 정상 쿼리(페이지 조회 수 초, 묶음 삭제 수십 초)보다 충분히 길다.
- **가드 테스트**: 위 임시 테스트를 정식 테스트로 둔다. 운영 jar 처럼 설정 파일을 한 폴더에 모아 prod 프로필로 띄우고 `socketTimeout` 이 의도한 값인지 확인한다. core-batch 테스트 리소스의 `application.yml` 이 main 의 것을 가리므로 `spring.config.location` 으로 그 폴더를 넘긴다. 이 결함은 설정 파일 위치만 바뀌어도 다시 생기므로 테스트로 고정한다.

### 4.2 H2. 순위형 1인 집계기가 지원하는 모드만 집계한다

- 운영 main 의 `BrawlerBattleRankStatsAggregator.aggregate` 첫머리에서 `battle.eventModeBrawlStarsId` 가 솔로 쇼다운(6)·듀오 쇼다운(9)·트리오 쇼다운(38)이 아니면 건너뛴다.
- 운영 main 에는 `StatsEventMode`(스펙 A)가 없어서 이 세 값을 집계기 안의 상수 집합으로 둔다. KDoc 에 "스펙 A 의 `StatsEventMode` 순위형 모드와 같은 목록이고, develop 병합 때 그쪽으로 바뀐다" 고 적는다.
- 모드 75(20인 솔로)와 86(10팀 듀오)의 이벤트는 순위형 1인 통계에 남지 않는다. 스펙 A 가 정한 수집 범위와 같다. 두 모드의 11-05 이후 통계는 지금도 없다(잡이 실패해서).
- **운영에서 빠지는 통계는 없다**(2026-11-10 운영 원본으로 확인). 11-02 원본의 순위형 모드는 6·9·38 뿐이고, 75 와 86 은 둘 다 11-05 에 처음 들어왔다. 11-04 까지 쓰인 순위형 통계에는 두 모드가 없다. 11-09 원본에서 순위형 모드는 이 다섯 개뿐이다(6: 30,520판, 75: 18,129, 9: 10,637, 86: 6,075, 38: 3,272).
- `battleRank > 10` 인 전투만 건너뛰는 방법은 쓰지 않는다. 모드 75 의 상위 10등만 남아 그 이벤트의 순위 분포가 틀린 값으로 저장된다.

### 4.3 H3. 잡이 실패하면 알린다

- core-batch 에 리스너 하나를 두고 두 이벤트를 받는다. 잡마다 따로 연결하지 않아도 모든 잡에 걸린다.
  - `JobExecutionEvent`(Spring Boot `org.springframework.boot.batch.autoconfigure`): `JobLauncherApplicationRunner.execute` 가 `jobOperator.start` 다음에 낸다. 잡이 시작된 뒤의 실패(§1 표의 순위형·카운터·2인)를 잡는다. 상태가 `COMPLETED` 가 아니면 알린다.
  - `ApplicationFailedEvent`: 잡이 시작되기 전에 러너가 예외를 던진 경우를 잡는다(§1 표의 이벤트 동기화, `InvalidJobParametersException`). 이때는 `JobExecutionEvent` 가 나오지 않는다(위 메서드에서 `start` 가 예외를 던지면 이벤트 줄에 닿지 않는다).
- 운영 main(Boot 4.0.2)과 develop(4.1.1) 모두 두 클래스가 있고, 러너 소스는 같다.
- 알림은 `AlertManager` 로 error 를 한 번 보낸다. 제목은 `[잡 이름] 실패`, 본문은 잡 파라미터(있으면), 종료 상태나 예외 클래스, 메시지 첫 줄, 파드 이름(`HOSTNAME`)이다. `DiscordAlertManager` 는 RestClient 로 동기 전송하므로 JVM 이 끝나기 전에 나간다.
- 알림을 만드는 중의 예외는 로그만 남긴다. 알림 때문에 잡의 종료 코드가 바뀌지 않게 한다.
- **한계**: 컨텍스트가 뜨기 전의 실패(DB 연결 실패로 기동이 멈춘 경우 등)는 `AlertManager` 빈이 없어 알리지 못한다.
- 쿠버네티스가 6번 재시도하므로 결정적인 오류는 하루에 알림 6개가 온다. 시끄럽지만 알리는 것이 먼저다. 줄이려면 CronJob 의 `backoffLimit` 을 낮춰야 하는데, 그 값은 저장소 밖 차트에 있다.
- 외부 API 때문에 가끔 실패하는 `rankingCollectJob`·`battleEventRotationSyncJob`(§1, 이 문서에서 고치지 않는 실패)에도 알림이 간다. §1 의 비율이면 일주일에 50건쯤이다. 잡 이름으로 빼거나 경고 수준으로 낮출지는 사용자 결정이다. 구현은 모든 잡을 똑같이 알린다.
- **기동 테스트로 확인했다**: 실제 core-batch 앱을 잡 실행 모드로 띄워, 필수 파라미터 없이 `battleEventSyncJob` 을 실행하면(시작 전 실패) 알림이 한 번, 스텝이 예외를 던지는 테스트 잡을 실행하면(시작 뒤 실패) 한 번 나가고, `healthCheckJob` 이 완료되면 나가지 않는다.

### 4.4 H4. 이벤트 동기화 잡을 되살린다

- `BattleEventSyncJobConfig` 에서 `.incrementer(RunIdIncrementer())` 를 지운다. 다른 `date` 잡처럼 CronJob 의 `date` 로 잡 인스턴스가 정해진다.
- develop 의 중복 경고 수정(`57c79875`)에서 `BattleEventSyncItemWriter` 두 줄을 함께 가져온다. 운영 main 의 Writer 는 확인 목록을 청크 반복이 끝난 뒤에야 채운다. 그래서 한 청크에 미등록 이벤트 전투가 N판이면 경고도 N건이 나간다. 미등록 이벤트가 44개인 지금 그대로 되살리면 Discord 경고가 수천 건이 될 수 있다.
- 수정 뒤에도 첫 실행에서 미등록·불일치 이벤트마다 경고가 간다. 순위형·결과형 스텝의 Writer 가 따로라서 이벤트 하나에 최대 두 건, 모두 수십 ~ 90건쯤이다. 이 경고는 스펙 A 가 기대한 동작이다(관리자가 보고 등록한다).
- **가드 테스트**: 컨텍스트의 모든 `Job` 빈에 대해 "빈 파라미터로 검증기를 부르면 예외가 나는 잡(필수 파라미터가 있는 잡)은 incrementer 가 없다" 를 확인한다. 이 결함은 incrementer 한 줄로 다시 생기기 때문이다.
- H4 는 H1 ~ H3 과 독립이다. 따로 내도 된다.

### 4.5 복구 런북

**기한**: 지금 운영은 원본 파티션을 10일 보존한다. 날짜 D 는 D+11 의 02:00 `partitionManageJob` 에서 지워진다. 아래 마감은 이 기준이다.

| 되살릴 것 | 필요한 원본 | 마감(보관 10일) |
|---|---|---|
| 조합·카운터 묶음 [11-02 ~ 11-08] | 11-02 ~ 11-08 | **11-13 02:00 전**(11-02 파티션이 지워진다) |
| 순위형 1인 11-05 | 11-05 | 11-16 02:00 전. 하루 늦을 때마다 가장 이른 날짜를 하나씩 잃는다 |

- **PR #85 가 먼저 배포되면 마감이 풀린다.** #85 는 보관을 45일로 늘린다. #85 가 11-13 02:00 전에 배포되면 11-02 파티션은 12-18 02:00 까지 남으므로, 위 두 마감 모두 이 hotfix 배포 일정을 서두를 이유가 아니게 된다. #85 가 11-13 02:00 을 넘기면 위 표가 그대로다(11-02 가 지워진 뒤에는 [11-02 ~ 11-08] 묶음을 온전히 되살릴 수 없다, §8).
- 마감이 풀려도 순위형 1인·카운터·2인은 이 hotfix 가 나갈 때까지 날마다 실패한다. 복구 순서는 아래와 같다.

**순서** (T = 배포한 날. 수동 실행은 통계 배치 시간대 02:00 ~ 09:00 KST 밖에서 한다)

1. hotfix 를 main 에 병합하고 core-batch 를 배포한다. CronJob 이미지 태그가 바뀌었는지 확인한다.
   - **실패 알림 확인(H3)**: 테스트는 모두 기록용 알림 대역으로 했으므로 Discord 로 실제로 나가는지는 운영에서 처음 본다. `battleEventSyncJob` 을 `date` 없이 한 번 실행한다(잡이 시작되기 전에 실패해 데이터는 바뀌지 않는다). Discord 에 `[battleEventSyncJob] 실패` 알림이 한 건 오고 파드가 0 이 아닌 종료 코드로 끝나는지 본다. 일회성 Job 으로 띄우면 재시도로 알림이 여러 건 오지 않게 `backoffLimit` 을 0 으로 둔다.
2. **카운터·2인**: 두 잡을 `date=2026-11-08` 로 한 번씩 실행한다. 11-08 이 일요일이라 [11-02 ~ 11-08] 묶음 하나만 다시 쓴다. 두 잡을 동시에 돌리지 말고 하나씩 돌린다(DB 부하가 겹친다). 잡마다 30 ~ 60분은 11-06 까지의 소요 시간에서 늘려 잡은 **추정**이다. 먼저 카운터 잡 하나를 돌려 `주간 범위 완료` 로그의 걸린 시간을 보고, 2인 잡의 시작 시각과 마감(11-13 02:00)을 정한다.
3. **카운터·2인 현재 묶음**: T 의 정기 실행(04:00, 07:00)이 옛 이미지였다면 두 잡을 `date` = T − 1 로도 실행한다. T − 1 이 속한 묶음([11-09 ~ 11-15] 등)을 쓴다. T − 1 이 묶음의 첫날(월요일 또는 1일)이면 직전 묶음도 함께 쓴다. 그 묶음의 시작일이 T − 10 이상인지 먼저 본다(전략 문서 10장). 이 "T − 10" 은 보관 10일 기준이다. PR #85(보관 45일)가 배포되면 하한이 넓어지므로 배포 순서가 정해진 뒤 다시 본다.
4. **순위형 1인**: `date` = 11-05, 11-06, …, T − 1 로 하루씩, 오래된 날짜부터 실행한다. 날짜 하나가 1 ~ 2분이다(전투 13 ~ 22만 건, 11-04 실행의 초당 약 2,400건 기준).
5. **이벤트 동기화(H4 를 냈다면)**: 다음 날 08:00 정기 실행을 기다리거나 `date` = T − 1 로 한 번 실행한다. 경쟁전 맵은 날마다 다 쓰이므로 하루치로 그 시즌의 `battle_event_solo_rank_event` 가 찬다. 여러 날짜를 더 돌리면 같은 미등록 경고가 날짜마다 반복되므로 하지 않는다.
6. **확인**: 실행 로그의 `COMPLETED`. `brawler_battle_rank_stats` 에 11-05 ~ T − 1 날짜가 생겼는지. 조합 묶음 [11-02 ~ 11-08] 의 `max(created_at)` 이 오늘인지(이벤트 하나로 조회, §1). 화면은 통계 캐시 TTL 30분 뒤에 본다.

- 실행 인자는 `java -jar /app/application.jar --job.name=<잡> date=YYYY-MM-DD` 다. 쿠버네티스에서 일회성 Job 으로 띄우는 절차는 저장소에 없다. 실행 방법은 운영자가 정한다(C1 런북과 같다).
- 이번에 다시 쓴 날짜·묶음은 그 뒤에 들어온 늦은 전투까지 담는다. 그래서 정기 실행으로 쓰인 다른 날짜보다 표본이 많다(스펙 D §1.3). 스펙 D 가 적용되면 모든 날짜가 같은 기준으로 맞춰진다.

### 4.6 develop 으로 backmerge

- hotfix 를 main 에 반영한 뒤 develop 으로 backmerge 한다(저장소 Git 전략).
- H1 은 그대로 들어간다. develop 의 `application.yml` 은 main 과 import 한 줄만 다르다.
- H2 는 develop 의 순위형 집계기(봇 로비 판정이 더해짐)와 충돌한다. develop 쪽은 상수 집합 대신 `StatsEventMode.find(...)?.resultType == RANK` 로 푼다(스펙 D 결정 5 와 같은 형태).
- H3 은 그대로 들어간다.
- H4 의 incrementer 삭제는 그대로 들어간다. Writer 두 줄은 develop 에 이미 있다(`57c79875`).
- 이렇게 하면 스펙 A·B·C1 릴리즈가 배포 다음 날 순위형 실패와 3초 제한을 다시 겪지 않고, 스펙 A 가 기대한 미등록 이벤트 경고도 실제로 나간다.

## 5. 안 D 를 고를 때의 조건

- develop 에 H1, 순위형 거름(스펙 D 결정 5), `battleEventSyncJob` 의 incrementer 삭제를 먼저 넣는다. 앞의 둘이 없으면 배포 다음 날부터 같은 실패가 나고, 마지막 것이 없으면 스펙 A 의 미등록 이벤트 경고가 나가지 않는다.
- 스펙 B §3.8 의 이관 순서와 C1 §3.4 런북을 11-12 안에 끝내야 [11-02 ~ 11-08] 묶음을 온전한 원본으로 다시 쓸 수 있다(C1 의 "시작일 ≥ T − 10").
- 순위형 1인은 C1 런북 2번(1인 재집계)이 11-05 ~ T − 1 을 메운다. 11-05 는 11-15 안에 해야 한다.

## 6. 바꾸지 않는 것

- 운영 main 의 조합 Reader 구조. 느리지만 3초 제한만 풀면 끝난다. 구조 개선은 스펙 B(develop)에 이미 있다.
- `rankingCollectJob`·`battleEventRotationSyncJob` 의 간헐적 실패(외부 API 429·503·시간 초과).
- 미등록 이벤트 44개의 등록(관리자 작업).
- 봇 전투, 허용 목록 밖 결과형 모드. 운영 main 의 동작 그대로다(스펙 A·C1 이 다룬다).
- 늦게 들어온 전투의 반영(스펙 D).
- CronJob 의 `backoffLimit`·`activeDeadlineSeconds`(저장소 밖 차트).

## 7. 테스트·검증

- **H1**: 설정 가드 테스트(§4.1). prod·dev 에서 `socketTimeout` 이 정한 값이고, `maximum-pool-size` 가 5 인지 확인한다.
- **H2**: 순위형 집계기 단위 테스트. 모드 75 이고 순위 19 인 전투는 건너뛰고 같은 날짜의 모드 6 전투는 집계하며, 쓰기에서 예외가 나지 않는다. 모드 86 도 건너뛴다. 기존 `BrawlerBattleRankStatsTaskTest` 는 모드 2 픽스처를 쓰므로 6 으로 바꾼다.
- **H3**: 리스너 단위 테스트(mockk). `JobExecutionEvent` 가 `FAILED` 면 알림을 한 번 보내고 제목에 잡 이름이 든다. `COMPLETED` 면 보내지 않는다. `ApplicationFailedEvent` 면 예외 클래스를 담아 한 번 보낸다. `AlertManager` 가 예외를 던져도 리스너는 정상 종료한다.
- **H4**: incrementer 가드 테스트(컨텍스트 테스트). Writer 의 중복 경고 테스트는 `57c79875` 의 `BattleEventSyncItemWriterTest` 사례를 가져온다(한 청크에 같은 미등록 이벤트 전투가 여럿이어도 경고 한 건).
- **실행**: `imstargg-backend` 에서 `./gradlew test`(컨텍스트 테스트 포함)와 `./gradlew ktlintCheck`.
- **구현에서 한 것(2026-11-10)**: 가드 테스트는 모두 고치기 전 main 에서 먼저 돌려 실패를 봤다. H1 가드는 네 프로필 모두 `3` 으로 실패했다. H2 는 운영과 같은 `rank(19)는 최대 10` 예외가 났다. H4 가드는 `battleEventSyncJob` 한 건을 잡았다. H3 기동 테스트는 알림 0건으로 실패했다. 고친 뒤 모두 통과한다. H1 가드가 모으는 설정 파일 여섯 개(core-batch 의 `application.yml`·`application-batch-db.yml`, db-core 의 `application-db.yml`·`application-db-lock.yml`, `logging.yml`, `brawlstars-client.yml`)는 실제 bootJar 안의 설정 파일과 같다.
- **배포 후**: §4.5 의 6번. 다음 날 02:00·04:00·07:00 정기 실행의 `COMPLETED` 와 소요 시간을 VictoriaLogs 로 본다.

## 8. 위험과 대응

| 위험 | 대응 |
|---|---|
| 3초 제한을 풀자 운영 main 의 조합 잡이 아주 오래 걸린다 | 11-06 까지 21분이었다. 묶음이 다 찬 날은 30 ~ 60분으로 추정한다. 600초 상한이 멈춘 쿼리를 끊는다. 이 구조는 develop 배포로 사라진다 |
| 3초 제한을 풀어도 DB 의 `statement_timeout` 이 쿼리를 끊는다 | **미확인**. 역할·DB 별 설정(`pg_db_role_setting`)은 없다. 서버 설정 파일 값은 읽기 전용 역할로 볼 수 없다(`pg_file_settings` 권한 없음). 첫 수동 실행(§4.5 의 2번)에서 드러난다. 끊기면 앱 연결의 `options` 로 세션 값을 정한다 |
| H4 로 되살린 잡이 미등록 이벤트 경고를 쏟아낸다 | 중복 수정(Writer 두 줄)을 함께 낸다. 그래도 첫 실행에 수십 ~ 90건쯤 온다. 관리자 등록으로 줄어든다 |
| 600초를 넘는 정상 쿼리가 있다 | 지금 가장 무거운 정상 쿼리는 조합 페이지(수 초)와 묶음 삭제다. 알림(H3)과 로그로 보고 값을 올린다 |
| 복구 실행이 11-13 02:00 을 넘긴다(PR #85 도 그 전에 배포되지 않은 경우) | [11-02 ~ 11-08] 묶음은 11-02 ~ 11-06 의 일부만 담은 값으로 남는다. 늦어지면 그 묶음은 `date=2026-11-08` 로 다시 쓰지 않는다(원본이 모자란 부분 데이터로 덮인다). #85 가 11-13 02:00 전에 배포되면 이 위험은 없다 |
| 실패 알림이 하루 6개씩 온다 | 받아들인다. 실패가 며칠씩 묻히는 것보다 낫다 |
| backmerge 충돌을 잘못 풀어 develop 순위형 거름이 빠진다 | §4.6 의 형태로 풀고, develop 에도 같은 단위 테스트를 둔다 |
