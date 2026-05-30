# Workflow Details

각 모드의 자세한 단계와 엣지 케이스 처리.

---

## bump 모드

### 트리거 컨텍스트

- 현재 브랜치가 feature/* 또는 develop
- 사용자가 작업 마무리 신호 ("PR 만들자", "커밋해줘", "버전 올려줘", "작업 끝났어") 를 보냈거나
- 명시 호출 (`/release bump`)

### 단계

1. **변경 감지**
   ```bash
   python3 scripts/detect_changed_apps.py --base main
   ```
   결과 JSON 의 `apps` 배열이 영향 받는 앱.

2. **변경 없으면 즉시 종료** — `apps` 가 비어있으면 "변경된 앱 없음" 한 줄 알림 후 끝.

3. **현재 버전 조회**
   ```bash
   python3 scripts/current_versions.py
   ```

4. **각 앱별 bump 종류 판단** (Claude 가 git diff 의 코드 변경 내용을 보고 결정)
   - 가이드: [version-rules.md](version-rules.md) 의 "Bump 종류 판단 가이드"
   - breaking change 의심되면 사용자에게 명시 확인 (무인 모드라도)

5. **각 앱별 dry-run 으로 결과 미리보기**
   ```bash
   python3 scripts/bump_version.py <app> <kind> --dry-run
   ```
   결과의 `changed: false` 인 앱은 변경 없음 (이미 같거나 큰 종류 SNAPSHOT).

6. **검수** (default)
   - 변경될 앱들의 `current → new` 표를 사용자에게 표시
   - 변경 없는 앱은 표에서 제외
   - AskUserQuestion 으로 승인 받기 (옵션: "적용", "수정", "취소")

7. **승인되면 실제 적용**
   ```bash
   python3 scripts/bump_version.py <app> <kind>   # without --dry-run
   ```

8. **git stage 까지가 default**. commit 은 사용자가 명시 요청 시.

### 무인 모드 (--auto)

- 단계 6 의 검수 생략, 즉시 적용
- 단, breaking 종류가 하나라도 있으면 무인 무시하고 검수 강제

---

## prepare 모드

### 트리거 컨텍스트

- 현재 브랜치가 develop (또는 main 으로 PR 준비 중)
- 사용자가 "main 으로 올리자", "릴리즈 준비", "main PR 만들자" 같은 신호
- 명시 호출 (`/release prepare`)

### 단계

1. **현재 버전 조회**
   ```bash
   python3 scripts/current_versions.py
   ```
   각 앱이 SNAPSHOT 인지 finalized 인지 확인. SNAPSHOT 앱들이 finalize 대상.

2. **모두 finalized 면 종료** — 새로 준비할 게 없음. (혹은 사용자에게 "이미 finalize 된 상태입니다" 안내)

3. **develop → main 의 변경 history 수집**
   ```bash
   git log main..HEAD --pretty=format:"%h %s"
   ```
   머지 커밋, PR 번호, conventional commits 가 있으면 활용.

4. **변경 사항 정리** (Claude 가 자연어 작성)
   - 앱별로 그룹화
   - 한 줄씩 사용자 친화적 문구로
   - PR 번호가 있으면 끝에 `(#123)` 형태로 인용

5. **CalVer 결정**
   ```bash
   ls docs/release/$(date +%Y.%m).*.md 2>/dev/null
   ```
   최대값 + 1 = 다음 N. 결과: `<YYYY>.<MM>.<N>`.

6. **검수** (default)
   - 결정된 CalVer
   - 각 앱의 `current(SNAPSHOT) → new(finalized)` 표
   - 변경 사항 초안 (markdown)
   - Breaking Changes / 마이그레이션 / 롤백 주의 — Claude 가 추측해서 채움 (없으면 "없음")
   - 사용자에게 검수 요청 → 노트 본문 수정 가능

7. **승인되면 실행**
   - 각 SNAPSHOT 앱에 대해:
     ```bash
     python3 scripts/finalize_version.py <app>
     ```
   - 노트 파일 생성:
     ```bash
     python3 scripts/new_release_note.py <calver> --apps '<JSON>' --changed '<JSON>'
     ```
     생성된 노트의 빈 칸 (변경 사항 본문, Breaking Changes 등) 을 Edit 으로 채움.
   - `docs/release/README.md` 의 인덱스에 한 줄 추가 (Edit tool, "## 인덱스" 섹션 아래 최상단에).

8. **git stage 까지가 default**. commit + PR 생성은 사용자 명시 요청 시.

---

## hotfix 모드

### 트리거 컨텍스트

- 현재 브랜치가 main (또는 hotfix 브랜치)
- 사용자가 "hotfix", "긴급 패치", "운영 버그 수정" 같은 신호
- 명시 호출 (`/release hotfix`)

### 단계

1. **사용자 확인**: 정말 hotfix 인지 (main 직접 패치 의도). develop 작업이라면 bump 모드로 가야 함.

2. **변경된 앱 식별**
   ```bash
   python3 scripts/detect_changed_apps.py --base main
   ```
   ※ 현재 main 에 있다면 working tree 변경만 잡힘.

3. **각 앱별 patch finalized bump** (SNAPSHOT 안 거침)

   현재가 finalized X.Y.Z 라면 직접 X.Y.(Z+1) 로:
   - bump_version.py 가 patch 로 SNAPSHOT 만들고
   - 곧바로 finalize_version.py 로 SNAPSHOT 떼기

   ```bash
   python3 scripts/bump_version.py <app> patch
   python3 scripts/finalize_version.py <app>
   ```

   현재가 SNAPSHOT 이라면 (있으면 안 됨, main 에는) → 사용자에게 경고.

4. **CalVer 결정** — 같은 달의 다음 N

5. **노트 생성** — 제목/내용에 "hotfix" 표시
   ```bash
   python3 scripts/new_release_note.py <calver> --apps '<JSON>' --changed '<JSON>'
   ```
   생성된 노트의 변경 사항 본문에 "hotfix: ..." 명시.

6. **develop 로 backmerge 안내**
   - develop 의 SNAPSHOT 과 main 의 finalized 가 충돌 가능
   - 사용자에게 "develop 에서 `git merge main` 후 충돌 해결 (대부분 SNAPSHOT 우선 유지)" 안내

---

## status 모드

읽기 전용. 파일 수정 X.

### 단계

1. `python3 scripts/current_versions.py` — 각 앱 현재 버전
2. `ls -t docs/release/*.md | head -1` — 마지막 릴리즈
3. `git log main..HEAD --merges --oneline` — develop 누적 머지 (또는 PR)
4. **다음 prepare 시 예상 결과 미리보기**:
   - SNAPSHOT 앱들의 finalized 후보 (X.Y.Z-SNAPSHOT → X.Y.Z)
   - 다음 CalVer 후보 (`YYYY.MM.<N+1>`)

5. 텍스트 요약 출력. 예:
   ```
   현재 버전:
     core-api    0.1.1-SNAPSHOT
     core-batch  0.1.0
     core-worker 0.1.0
     frontend    0.2.0-SNAPSHOT

   마지막 릴리즈: 2026.05.0 (2026-05-02)
   develop 누적: 3 PRs

   다음 prepare 시 예상:
     core-api  → 0.1.1
     frontend  → 0.2.0
     CalVer    → 2026.05.1
   ```

---

## 엣지 케이스 가이드

| 케이스 | 처리 |
|---|---|
| 변경된 앱 없음 (docs 만 수정 등) | bump: "변경 없음" 알림, 종료 |
| 라이브러리 모듈 변경만 | 의존하는 모든 bootable (core-api/core-batch/core-worker) 에 같은 종류 bump |
| 모든 앱 finalized 상태 (이전 prepare 직후) | bump: 모두 SNAPSHOT 으로 전환 / prepare: "이미 finalized" 종료 |
| 이미 격상된 SNAPSHOT 에 더 작은 변경 | 변경 없음 (현 버전 유지) |
| 같은 종류 SNAPSHOT 에 같은 종류 변경 | 변경 없음 (idempotent) |
| 새 앱이 모노레포에 추가됨 | `_config.py` 의 APPS 와 `docs/release/README.md` 의 표 동시 업데이트 |
| docs/release/ 같은 달의 N 결정 | 기존 파일들 검사 → 최대값 + 1 |
| breaking change 의심 | 사용자 명시 confirm 후 major 적용 |
| --auto 모드에서 breaking 감지 | --auto 무시, 검수 강제 |
| 사용자가 SNAPSHOT 인 앱을 직접 finalize 요청 (prepare 외) | hotfix 모드인지 재확인. 일반적으로는 prepare 가 정답 |
| 같은 PR 에 frontend + backend 변경 | 각각 독립적으로 bump 종류 판단. 같은 kind 일 필요 없음 |

---

## 안전 원칙 (모든 모드 공통)

1. **파일 수정 + git stage** 까지가 default. commit, push, PR 생성은 사용자 명시 요청 시.
2. **무인 모드라도 breaking change 는 검수 강제**.
3. **변경 없는 앱은 무시** — 노이즈 방지, idempotent.
4. **의심스러우면 보수적으로** — 작은 bump, 검수 모드 fallback.
5. **새 파일 생성 전 존재 여부 확인** — 같은 CalVer 의 노트 파일 중복 방지.
