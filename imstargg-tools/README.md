# imstargg-tools

Claude Code·Codex 등에서 사용할 프로젝트 전용 도구 공간이다. 현재 PostgreSQL CLI만 제공하며, 향후 필요한 도구를 이 디렉터리에 추가한다. MCP 서버나 별도 서비스는 실행하지 않는다.

## 보호 범위

기본 구성은 **별도 파일 + Git 제외 + 에이전트 읽기 금지 지침**이며, Claude Code에는 **Read/Edit 거부 설정**도 적용한다. 임의 셸 하위 프로세스를 포함한 모든 파일 접근을 차단하는 구성은 아니다. `chmod 600`은 다른 OS 사용자의 접근을 제한하는 설정이다.

실제 접속정보(호스트·DB명·계정·비밀번호)는 `.secrets/`에서만 관리한다. 예시 설정에는 가짜 값만 포함한다. 에이전트 행동 지침은 `AGENTS.md`에 있다.

```text
imstargg-tools/
├── AGENTS.md
├── bin/db                         # 에이전트 실행 명령
├── secrets.example/               # 커밋되는 예시(가짜 값), .secrets/와 같은 파일 이름
│   ├── pg_service.conf
│   └── pgpass
└── .secrets/                      # Git 제외, 사용자 직접 편집
    ├── pg_service.conf            # 호스트·DB명·계정
    └── pgpass                     # 비밀번호
```

## 사용자 설정

macOS 클라이언트 설치(서버 설치 아님):

```sh
brew install libpq
```

저장소 루트에서 `secrets.example/`의 예시를 `.secrets/`로 복사한다. 기존 파일은 덮어쓰지 않으므로 도구가 추가된 뒤 다시 실행해도 된다.

```sh
mkdir -p imstargg-tools/.secrets
chmod 700 imstargg-tools/.secrets
for f in imstargg-tools/secrets.example/*; do
    dest=imstargg-tools/.secrets/$(basename "$f")
    test -e "$dest" || cp "$f" "$dest"
done
chmod 600 imstargg-tools/.secrets/*
```

사용자가 로컬 편집기로 두 파일을 직접 수정한다. 비밀값을 에이전트 대화나 셸 명령 인수에 넣지 않는다.

1. `pg_service.conf`: `[live]`의 host, port, dbname, user를 실제 값으로 바꾼다. 환경이 늘어나면 서비스 섹션을 추가하고 `AGENTS.md`의 서비스 목록도 갱신한다. 비밀번호는 넣지 않는다. `options`의 읽기 전용·`statement_timeout` 기본값은 필요에 맞게 조정한다.
2. `pgpass`: `hostname:port:database:username:password` 형식으로 대응하는 항목을 추가한다. 값의 `:`와 `\`는 `\`로 이스케이프한다. 과도한 와일드카드는 피한다.
3. 원격 DB는 공급자 인증서에 맞춰 `sslmode=verify-full` 및 CA 설정을 구성한다.
4. 사용자 터미널에서 아래 명령으로 연결을 확인한다.

```sh
./imstargg-tools/bin/db live -c 'SELECT 1;'
```

`live`는 운영 DB다. `options`의 읽기 전용 기본값은 `SET`으로 해제할 수 있는 세션 설정이므로, 쓰기를 확실히 막으려면 SELECT 권한만 가진 전용 계정을 사용한다.

## 사용 예시

```sh
./imstargg-tools/bin/db --help
./imstargg-tools/bin/db live -c 'SELECT 1;'
./imstargg-tools/bin/db live -f path/to/query.sql
printf 'SELECT 1;\n' | ./imstargg-tools/bin/db live
```

스크립트 자신의 위치로부터 설정 경로를 계산하므로 현재 작업 폴더에 의존하지 않는다. `psql`은 PATH에서 찾고, macOS의 Homebrew libpq 설치도 찾아 사용한다.

`-X`로 개인 psql 시작 파일을 읽지 않고, `-w`로 비밀번호 입력을 기다리지 않으며, SQL 오류 시 실패 상태로 종료한다. 기존 `PGPASSWORD`는 제거하고 프로젝트 비밀번호 파일을 사용한다. 실제 설정 파일이 없으면 사용자 홈의 설정으로 넘어가지 않고 종료한다. 별도 `.env`를 source하지 않는다.

psql에 전달하는 옵션으로 연결 대상이나 동작을 바꿀 수 있으므로 이 스크립트는 보안 경계가 아니다. 보호 대상은 비밀번호이며, 연결 오류에 호스트·계정이 표시되는 것은 허용한다.

## 접근 제한 설정

`.claude/settings.json`의 `permissions.deny`에 다음 규칙을 적용한다. 절대 경로의 와일드카드를 사용해 저장소 하위 폴더에서 시작한 세션이나 다른 checkout 경로에서도 같은 도구 디렉터리를 대상으로 한다.

```json
{
  "permissions": {
    "deny": [
      "Read(//**/imstargg-tools/.secrets/**)",
      "Edit(//**/imstargg-tools/.secrets/**)"
    ]
  }
}
```

이는 Claude의 파일 도구와 인식 가능한 파일 명령에 적용되지만, 파일을 간접적으로 읽는 임의의 Python·Node 등 하위 프로세스를 모두 차단하지는 않는다. 실제 적용은 새 세션의 `/permissions`에서 확인한다.

Codex의 `permissions.<name>.filesystem` `deny`나 Claude의 `sandbox.filesystem.denyRead` 같은 샌드박스 차단은 같은 샌드박스에서 실행하는 psql에도 적용되므로, 현재 CLI의 연결 기능을 유지하기 위해 활성화하지 않았다.

공식 문서: [Claude 권한](https://code.claude.com/docs/en/permissions#read-and-edit), [Claude 샌드박스](https://code.claude.com/docs/en/sandboxing), [Codex 권한](https://developers.openai.com/codex/permissions).
