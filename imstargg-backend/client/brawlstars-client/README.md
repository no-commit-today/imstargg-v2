# brawlstars-client

브롤스타즈 공식 API(`https://api.brawlstars.com`)의 OpenFeign 클라이언트. core-worker·core-batch 가 사용한다 (core-api 는 사용하지 않는다).

## 호출 한도 (rate limit)

- imstargg 의 브롤스타즈 API 호출 한도는 **초당 약 50회**다.
- 앱 쪽 제한은 resilience4j 레이트리미터 `brawlstars`([brawlstars-client.yml](src/main/resources/brawlstars-client.yml))가 `BrawlStarsApi` 의 모든 호출에 건다.
  - prod 초당 10회, 그 외 프로필 초당 1회. 허가를 받을 때까지 최대 600초 대기한다.
- 리미터는 **JVM(파드)마다 따로** 걸린다. 실제 총 호출량은 동시에 떠 있는 프로세스의 합이다 — core-worker replica 와 실행 중인 core-batch 잡 파드가 각각 최대 초당 10회.
  - replica 를 늘리거나 리미터 값을 올릴 때는 이 합이 초당 50회를 넘지 않게 한다.
- 2026-10 기준 실사용량 (prod):
  - core-worker 통계 전투 수집: 초당 약 8.3회 (`app.schedule.stats-battle`, 600ms 마다 5명). 같은 리미터를 사용자 갱신 요청과 나눠 쓴다.
  - core-batch 랭킹 수집(`rankingCollectJob`, 30분 주기): 실행 중 초당 0.3~0.5회.
- 한도를 넘으면 API 가 429 를 돌려주고 `BrawlStarsApiClientException.TooManyRequests` 로 변환된다.

## API 키

`app.client.brawlstars.api-keys` 의 키를 요청마다 라운드로빈으로 돌려 쓴다 ([BrawlStarsApiKeyInterceptor](src/main/kotlin/com/imstargg/client/brawlstars/BrawlStarsApiKeyInterceptor.kt)).
