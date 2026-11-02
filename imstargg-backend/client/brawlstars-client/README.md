# brawlstars-client

## 호출 한도

imstargg 의 브롤스타즈 API 호출 한도는 초당 약 50회다.

## 구성

- HTTP 설정(요청 팩토리 Apache HttpClient 5·타임아웃·커넥션 풀·재시도 끔·호출 로깅)은 `support:http-client` 가 맡는다 — 앱이 `http-client.yml` 을 import 한다. 이 그룹(`brawlstars`)만 다르게 할 수 있는 것은 `spring.http.serviceclient.brawlstars.*` 의 타임아웃(`connect-timeout`·`read-timeout`) 등이다. 요청 팩토리 종류·커넥션 풀·재시도 끔은 앱 전역(`http-client.yml`·`HttpClientConfig`)이고, base-url 은 `app.client.brawlstars.url` 에서 온다.
- 레이트리밋은 `BrawlStarsRateLimitInterceptor` 가 HTTP 요청마다 resilience4j 제한기 `brawlstars`(`brawlstars-client.yml` 의 `resilience4j.ratelimiter.instances.brawlstars`)에서 허가를 하나씩 받는다.
- 여러 플레이어의 battlelog 는 배치 메서드 `getPlayersRecentBattles` 로 가상 스레드에서 함께 조회한다 — 클라이언트 전체 동시 실행 상한은 `app.client.brawlstars.batch-concurrency`(기본 5)이고, 태그별 결과를 `Result` 로 돌려준다.
