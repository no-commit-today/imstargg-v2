# SQS 큐 설정

플레이어/클럽 갱신 요청을 비동기 처리하기 위해 SQS를 사용한다.

## 큐 목록

| 환경 | Player 큐 | Club 큐 |
|------|-----------|---------|
| local | `local-player-renewal` | `local-club-renewal` |
| dev | `dev-player-renewal` | `dev-club-renewal` |
| prod | `prod-player-renewal` | `prod-club-renewal` |

- local 환경은 LocalStack 사용, `LocalAwsSqsInitializer`가 자동 생성
- dev/prod 환경은 AWS 콘솔에서 수동 생성

## 큐 생성 설정

| 설정 | 값            |
|------|--------------|
| Type | Standard     |
| Visibility Timeout | 30초 (기본값)    |
| Message Retention Period | 4일 (기본값)     |
| Delivery Delay | 0초           |
| Maximum Message Size | 256KiB (기본값) |
| Receive Message Wait Time | 20초          |
| Encryption | SSE-SQS      |

## DLQ

설정하지 않음. Consumer에서 모든 예외를 catch하고 상태를 FAIL로 변경한 뒤 정상 리턴하므로, 메시지가 삭제되지 않는 경우가 발생하지 않는다.
