---
name: release-management
description: 이 모노레포(imstargg-v2)의 SemVer + SNAPSHOT 버전 관리와 CalVer 묶음 릴리즈 노트를 자동화. 사용자가 코드를 수정한 뒤 'PR 만들자', '커밋해줘', '작업 끝났어', 'main 으로 올리자', 'bump', 'release', 'SNAPSHOT', '버전' 같은 마무리/배포 신호를 보내면 즉시 트리거 — 명시 호출 없이도. 변경된 앱(core-api/core-batch/core-worker/frontend)을 git diff 로 자동 감지하고 변경 종류(patch/minor/major)를 판단해 적용. 변경 없는 앱은 조용히 무시 (idempotent — 안전하게 반복 호출 가능). main PR 단계에서는 SNAPSHOT 을 제거하고 docs/release/ 에 묶음 노트를 생성. core-admin 은 관리 대상 외.
---

# Release Management

이 모노레포(imstargg-v2)의 버전 관리와 릴리즈 노트를 자동화한다. 사용자가 작업 마무리·PR 준비·배포 신호를 보내면 자체 판단으로 트리거된다.

## 모드 분기 (현재 컨텍스트로 자동 판단)

| 컨텍스트 | 모드 |
|---|---|
| feature/* 또는 develop 브랜치 + 코드 수정 후 마무리 신호 | **bump** |
| develop → main PR 준비 (배포 전) | **prepare** |
| main 브랜치에서 직접 hotfix | **hotfix** |
| 조회만 ("지금 버전 뭐야?", "다음 릴리즈 미리보기") | **status** |

명시 호출도 가능 — 슬래시 명령 `/release [bump|prepare|hotfix|status] [--auto]` 또는 자연어 ("버전 올려줘", "main PR 준비해줘"). 슬래시 명령은 [.claude/commands/release.md](.claude/commands/release.md) 에서 등록.

## 자동화 토글

- **검수 모드 (default)**: 변경 제안 표시 → 사용자 1회 확인 (AskUserQuestion) → 적용
- **무인 모드**: `--auto` 플래그 또는 사용자가 "바로 적용해줘" / "그대로 진행" 같은 표현 사용
- **breaking change 의심 시**: --auto 무시하고 검수 모드 강제

## 모노레포 매핑

스크립트들이 hardcode 로 가짐 (`scripts/_config.py`). 새 앱 추가 시 `_config.py` 의 `APPS` 와 `docs/release/README.md` 의 표 동시 업데이트.

| 앱 | 위치 | 키 |
|---|---|---|
| core-api | imstargg-backend/gradle.properties | coreApiVersion |
| core-batch | imstargg-backend/gradle.properties | coreBatchVersion |
| core-worker | imstargg-backend/gradle.properties | coreWorkerVersion |
| frontend | imstargg-frontend/package.json | version |

core-admin / imstargg-admin-frontend 는 관리 대상 외.

## bump 모드 워크플로

1. **변경 감지** — `python3 .claude/skills/release-management/scripts/detect_changed_apps.py` (base 기본값 `origin/main`. 손대지 않은 앱이 잡히거나 `base_resolved: false` 면 [workflow.md](references/workflow.md) 의 1단계 주의사항 참고)
2. **변경된 앱 없으면 조용히 종료** (한 줄 알림 후 끝). 노이즈 방지.
3. **현재 버전 조회** — `python3 .claude/skills/release-management/scripts/current_versions.py`
4. **각 앱별 bump 종류 판단** — git diff 의 코드 변경을 보고 patch/minor/major 결정. 가이드: [references/version-rules.md](references/version-rules.md)
   - 의심스러우면 보수적 (작은 쪽)
   - breaking change 의심되면 사용자 명시 confirm 필수 (무인 모드라도)
5. **dry-run 으로 결과 미리보기** — 각 앱별로:
   ```
   python3 .claude/skills/release-management/scripts/bump_version.py <app> <kind> --dry-run
   ```
   `changed: false` 인 앱은 표에서 제외 (이미 같거나 큰 종류 SNAPSHOT).
6. **검수 모드**: 변경될 앱 표 (`current → new`) 를 보여주고 AskUserQuestion 으로 승인
7. **승인되면 실제 적용** — 같은 명령에서 `--dry-run` 빼고 실행
8. **git stage 까지가 default**. commit/push/PR 은 사용자 명시 요청 시.

## prepare 모드 워크플로

1. **현재 버전 조회** — SNAPSHOT 인 앱들이 finalize 대상
2. **모두 finalized 면 종료** ("이미 finalize 된 상태입니다")
3. **develop → main 변경 history** — `git log main..HEAD --pretty=format:"%h %s"` (또는 `--merges` 로 PR 만)
4. **변경 사항 자연어 정리** — 앱별 그룹화, 사용자 친화 문구
5. **CalVer 결정** — `ls docs/release/<YYYY>.<MM>.*.md` 의 최대 N + 1
6. **검수 모드**: 결정된 CalVer + finalize 표 + 노트 초안 (Breaking/마이그레이션/롤백 섹션 포함) → 사용자 검수
7. **승인되면 실행**:
   - 각 SNAPSHOT 앱 finalize: `python3 .claude/skills/release-management/scripts/finalize_version.py <app>`
   - 노트 파일 생성: `python3 .claude/skills/release-management/scripts/new_release_note.py <calver> --apps '<JSON>' --changed '<JSON>'`
   - 생성된 노트의 빈 섹션 (변경 사항 본문, Breaking 등) Edit 으로 채움
   - `docs/release/README.md` 의 인덱스에 한 줄 추가 (Edit, "## 인덱스" 아래 최상단)
8. commit/push/PR 은 사용자 명시 요청 시.

## hotfix 모드 워크플로

1. **사용자 확인**: 정말 hotfix 인지 (main 직접 패치 의도)
2. **변경된 앱 식별** — detect_changed_apps.py
3. **각 앱 patch finalized bump** (SNAPSHOT 안 거침):
   ```
   python3 .claude/skills/release-management/scripts/bump_version.py <app> patch
   python3 .claude/skills/release-management/scripts/finalize_version.py <app>
   ```
4. **노트 생성** — hotfix 표시
5. **develop 로 backmerge 안내** — develop 의 SNAPSHOT 과 충돌 가능성

## status 모드 워크플로

읽기 전용. 다음을 텍스트로 요약:
- 각 앱 현재 버전 (current_versions.py)
- 마지막 릴리즈 (`ls -t docs/release/*.md | head -1`)
- develop 누적 PR 수 (`git log main..HEAD --merges --oneline | wc -l`)
- 다음 prepare 시 예상 (SNAPSHOT 앱들의 finalized 후보 + 다음 CalVer)

## 안전 원칙

- **파일 수정 + git stage** 까지가 default. commit/push/PR 은 사용자 명시 요청 시.
- **무인 모드라도 breaking change 는 검수 강제**.
- **변경 없는 앱은 무시** — idempotent, 반복 호출 안전.
- **의심스러우면 보수적**: 작은 bump, 검수 fallback.
- **노트 파일 중복 방지**: 같은 CalVer 가 이미 있으면 스크립트가 exit non-zero.

## 자세한 동작 / 엣지 케이스

자세한 모드별 단계와 엣지 케이스는 [references/workflow.md](references/workflow.md) 참고. SNAPSHOT 매트릭스와 bump 종류 판단 가이드는 [references/version-rules.md](references/version-rules.md).
