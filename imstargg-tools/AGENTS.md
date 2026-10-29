# imstargg-tools

프로젝트 에이전트용 CLI와 사용 지침. 현재 PostgreSQL만 제공한다.

## 인증정보 취급

- `.secrets/`는 사용자만 편집하는 접속정보 저장 영역이다. 에이전트는 파일 열람·검색·출력·복사·수정을 하지 않는다. 숨김/ignored 파일을 포함하는 전체 검색에서도 제외한다.
- 최초 설치 때만 `secrets.example/`의 예시를 복사해 비어 있는 설정을 생성할 수 있다. 기존 파일을 읽거나 덮어쓰지 않는다.
- 인증정보를 `cat`, `rg`, `source`, 환경변수 덤프, 디버그 추적(`set -x`, `sh -x`) 등으로 읽거나 출력하지 않는다.
- 실제 비밀번호·호스트·계정을 채팅으로 요청하지 않는다. 설정은 사용자가 직접 한다.
- `.secrets/` 경로는 CLI에 전달하며, 내용은 `psql`/libpq가 읽게 한다. 이 구분은 작업 지침이며 기술적 접근 차단이 아니다.
- 접속 문자열, 비밀번호 인수, `PGPASSWORD`로 로컬 설정을 우회하지 않는다.
- `.secrets/` 파일을 Git에 강제 추가하지 않는다. 예시에는 가짜 값만 사용한다.
- Claude Code의 Read/Edit 거부 규칙은 `.claude/settings.json`에 있다. 거부된 파일을 셸·Python 등 다른 경로로 읽으려 하지 않는다. 이 규칙은 모든 하위 프로세스의 읽기를 막는 샌드박스 차단과는 다르다.

## PostgreSQL 사용

저장소 루트 기준:

```sh
./imstargg-tools/bin/db live -c 'SELECT 1;'
./imstargg-tools/bin/db live -c 'SELECT * FROM information_schema.tables LIMIT 20;'
```

- 사용 가능한 서비스는 `live`(운영 DB) 하나다. 다른 서비스가 필요하면 사용자에게 묻고, 파일을 열어 이름을 찾지 않는다.
- 운영 DB이므로 인덱스를 타지 않는 대량 스캔·장시간 쿼리를 피한다. 큰 테이블은 조건과 LIMIT를 먼저 확인하고 필요하면 `EXPLAIN`으로 계획을 본다.
- 기본 작업은 조회다. 필요한 컬럼과 LIMIT를 사용하고 대량 출력·민감 데이터 조회를 피한다.
- 서비스 설정에 읽기 전용(`default_transaction_read_only`)·실행 시간 제한(`statement_timeout`) 기본값이 걸려 있을 수 있다. 쓰기 거부나 시간 초과가 나면 임의로 해제하지 않는다.
- DDL/DML은 사용자가 해당 변경을 요청한 경우에만 `BEGIN READ WRITE; ... COMMIT;`으로 수행한다. 읽기 전용 기본값은 세션 설정일 뿐 쓰기를 강제로 막지 않는다.
- 연결 실패 시 사용자에게 로컬 설정·네트워크·DB 권한 확인을 요청한다. 설정 파일을 대신 읽지 않는다.
