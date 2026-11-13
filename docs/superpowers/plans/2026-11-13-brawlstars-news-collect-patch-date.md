# 공식 뉴스 수집·알림과 패치일 등록 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 공식 블로그의 글을 하루 두 번 받아 판 단위로 저장하고 새 판마다 Discord 로 알리며, 운영자가 어드민에서 글 이력을 보고 패치일을 등록하게 한다.

**Architecture:** db-core 에 글(한 행 = 한 판)과 패치일 엔티티를 둔다. core-batch 의 새 잡이 전용 HTTP 클라이언트로 블로그를 읽고, 순수 로직(주소 키, 글자 뽑기, 유형 판정, 알림 문구)으로 직전 판과 견줘 저장하고 알린다. core-admin 은 조회·유형 수정·패치일 관리 API 를, admin-frontend 는 화면 셋을 더한다. 공용 Discord 발송기는 전송 실패를 던지도록 고친다.

**Tech Stack:** Kotlin + Spring Boot 4, Spring Batch 6, JPA(테스트는 H2), RestClient + MockRestServiceServer, Jackson 3, MockK, MockMvc. Next.js 16 + React 19, shadcn(base-ui), sonner, `diff`.

**Spec:** [docs/superpowers/specs/2026-11-11-brawlstars-news-collect-patch-date-design.md](../specs/2026-11-11-brawlstars-news-collect-patch-date-design.md). 이 계획이 스펙과 어긋나면 스펙이 이긴다.

## Global Constraints

- `./gradlew` 는 `imstargg-backend/`, `pnpm` 은 `imstargg-admin-frontend/` 에서 실행한다. **`git` 은 저장소 루트에서 실행한다.** 아래 경로는 모두 저장소 루트 기준이다.
- **이름**(스펙 §4, §9): 테이블 `brawlstars_news_article`, `brawlstars_patch`. 글 유형 `RELEASE_NOTE`, `OTHER`. 잡 `brawlStarsNewsCollectJob`. CronJob `brawlstars-news-collect`, 스케줄 `20 0,12 * * *`(UTC).
- **상수**: 읽는 목록 쪽 수 3, 등록 기한 30일, 요청 사이 쉬는 시간 0.1초에서 1초 사이 무작위, 사이트 주소 `https://supercell.com`, 목록 경로 `/en/games/brawlstars/blog/` 와 `/en/games/brawlstars/blog/page/{N}/`, 글 페이지의 `page` 값 `/en/games/brawlstars/blog/[category]/[title]`.
- **정규식**: 주소 키 `-\d{1,2}$`(한 번만 뗀다). 유형 `release notes|patch notes|\bupdate \d+\b`(대소문자 무시).
- **오류**(스펙 §8): 예외를 잡아 넘기지 않는다. 새 잡은 `BrawlStarsApiTasklet` 을 쓰지 않는다. 이 잡만의 실패 알림을 만들지 않는다(공통 `JobFailureAlertListener` 가 알린다).
- **테스트**: DB 는 H2 실 DB 를 쓰고 리포지토리를 mockk 로 바꾸지 않는다. 외부 HTTP 는 `MockRestServiceServer` 로, 알림은 `mockk<AlertManager>(relaxUnitFun = true)` 또는 `@MockkBean(relaxUnitFun = true)` 로 대신한다. 공식 블로그나 Discord 에 실제 요청을 보내는 테스트를 만들지 않는다. 컨텍스트 테스트 서브클래스에 `@Tag("context")` 를 다시 달지 않는다.
- **공개 저장소**: 공식 글의 원문을 저장소에 넣지 않는다. 픽스처는 글자를 가린 사본을 쓴다(Task 3).
- **프로젝트 규칙**: 파일당 클래스 하나, 운영 코드에서 인자가 셋 이상이면 named arguments, 주석·KDoc·테스트 이름은 한국어, 로깅은 kotlin-logging. `@OneToMany` 금지, Service 가 다른 Service 를 부르지 않는다. 스키마는 `schema.sql` 과 마이그레이션 파일 양쪽에 멱등하게 쓰고, 인덱스는 엔티티 `@Table` 에도 적는다. 외래 키 제약은 걸지 않는다.
- Kotlin 을 고치기 전에 `superdomain:conventions` 스킬을 읽는다. 새 이름은 `docs/superglossary/glossary.md` 의 단어로 짓는다(Task 1 이 필요한 단어를 등록한다).
- 커밋은 Conventional Commits 이고 끝에 `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>` 트레일러를 붙인다. `git add` 는 경로를 적어서 한다(`.agents/skills/release-management/` 는 이 작업의 파일이 아니다).
- 운영 DB 와 `.secrets/` 에 접근하지 않는다.

## 실행 전 확인 (컨트롤러)

- **브랜치**: 지금 브랜치에는 문서 커밋만 있다. 브랜치 이름을 `feature/brawlstars-news-patch-date` 로 바꾸고 그 위에서 구현한다.
- **커밋 시각**: 09시에서 18시 사이(KST)에 찍힌 커밋은 푸시·PR 전에 그 밖의 시각으로 옮긴다.
- **core-domain**: `imstargg-backend/core/core-domain` 모듈이 생겼으면 멈추고 사용자에게 알린다. 아래 File Structure 의 "순수 규칙의 자리"가 그 모듈의 규칙으로 바뀐다.
- **공통 잡 실패 알림**: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/support/alert/JobFailureAlertListener.kt` 가 있어야 한다. 이 계획은 그것이 모든 잡의 실패를 알린다는 전제로 썼다.
- **패치일 초기값**: Task 2 의 마이그레이션에 넣을 패치일 목록을 사용자에게 확인한다. 계획을 쓴 시점에는 2026-11-03, 2026-11-11 이다.
- **admin-frontend**: `node_modules` 가 없으면 `pnpm install` 을 먼저 한다. `pnpm` 이 "env: node: No such file" 로 죽으면 `/opt/homebrew/bin/pnpm` 을 직접 부른다. 코드를 쓰기 전에 `imstargg-admin-frontend/AGENTS.md` 와 그 문서가 가리키는 Next 문서를 읽는다.

## Review Focus

1. **문자 인코딩을 알려 주지 않는 응답의 한글·이모지**: 깨지지 않고 읽힌다. → Task 4 "문자 인코딩 표시가 없어도 한글과 이모지를 읽는다".
2. **같은 주소 키의 서로 다른 주소 두 건이 목록에 함께 있음**(`update65-2` 와 `update65-3`): 두 번째 실행에서 저장도 알림도 없다. → Task 7 "같은 주소 키의 두 주소가 함께 있어도 두 번째 실행은 조용하다".
3. **알림 전송이 실패한 실행 뒤의 재실행**: 저장된 판은 넘어가고 그 알림은 다시 나가지 않는다. 저장 전이던 글은 이어서 저장하고 알린다. → Task 7 "알림 전송이 실패하면 예외가 나고 저장은 남는다", "실패한 실행 뒤에 다시 돌리면 남은 글만 저장하고 알린다".
4. **Discord 연결 오류의 예외 메시지**: 웹훅 주소가 들어가지 않는다. 들어가면 잡 실패 알림 본문과 배치 실행 기록에 남는다. → Task 5 "연결 오류의 예외에는 웹훅 주소가 없다".
5. **옛 날짜가 기한을 넘긴 패치일의 수정**: 메모만 고치면 통과하고 날짜를 바꾸면 400 이다. → Task 9 "옛 날짜가 기한 밖이면 메모만 고칠 수 있다".

## File Structure

`…db/` 는 `imstargg-backend/infrastructure/db-core/src/{main,test}/kotlin/com/imstargg/infrastructure/db/core/`, `…batch/` 는 `imstargg-backend/core/core-batch/src/{main,test}/kotlin/com/imstargg/core/batch/`, `…admin/` 은 `imstargg-backend/core/core-admin/src/{main,test}/kotlin/com/imstargg/core/admin/`, `fe/` 는 `imstargg-admin-frontend/` 다.

| 파일 | 책임 |
|---|---|
| `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/BrawlStarsNewsArticleType.kt` | 글 유형 |
| `…db/brawlstars/news/BrawlStarsNewsArticleLink.kt` | 주소 값 타입. 절대 주소와 주소 키를 만든다 |
| `…db/brawlstars/news/BrawlStarsNewsArticleEntity.kt` | 글의 한 판 |
| `…db/brawlstars/news/repository/BrawlStarsNewsArticleJpaRepository.kt`, `BrawlStarsNewsArticlePreviousFinder.kt` | 조회와 "직전 판 찾기" 규칙 |
| `…db/brawlstars/patch/BrawlStarsPatchEntity.kt`, `BrawlStarsPatchRegistrationPolicy.kt`, `repository/BrawlStarsPatchJpaRepository.kt` | 패치일과 등록 기한 규칙 |
| `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`, `db/migration/V{yyMMdd}a__…`, `V{yyMMdd}b__…` | DDL 과 패치일 초기값 |
| `…batch/domain/news/NewsArticleContentExtractor.kt` | 글자 뽑기 |
| `…batch/domain/news/NewsArticleTypeClassifier.kt`, `NewsArticleContentDiff.kt`, `NewsArticleAlertMessage.kt` | 유형 판정, 줄 수 세기, 알림 문구 |
| `…batch/support/blog/BrawlStarsBlogClient.kt`, `BrawlStarsBlogArticleSummary.kt`, `BrawlStarsBlogPageException.kt` | 블로그 읽기와 응답 검증 |
| `…batch/support/alert/DiscordAlertManager.kt`, `AlertConfig.kt` | 발송기 변경 |
| `…batch/job/brawlstars/task/BrawlStarsNewsCollectTask.kt` | 수집 흐름 |
| `…batch/job/brawlstars/BrawlStarsNewsCollectJobConfig.kt` | 잡 설정 |
| `…admin/controller/brawlstars/AdminBrawlStarsPatchController.kt`, `…admin/application/brawlstars/AdminBrawlStarsPatchService.kt` | 패치일 API |
| `…admin/controller/brawlstars/AdminBrawlStarsNewsArticleController.kt`, `…admin/application/brawlstars/AdminBrawlStarsNewsArticleService.kt`, `AdminBrawlStarsNewsArticleRepository.kt` | 글 API |
| `…admin/request/brawlstars/*`, `…admin/response/brawlstars/*` | 요청·응답 DTO |
| `fe/lib/types.ts`, `fe/lib/api/brawlstars/patch.ts`, `news-article.ts` | 타입과 API 함수 |
| `fe/app/patches/page.tsx`, `fe/app/patches/_components/PatchFormDialog.tsx` | 패치일 화면 |
| `fe/app/news-articles/page.tsx`, `[id]/page.tsx`, `_components/NewsArticleTypeSelect.tsx`, `NewsArticleDiffView.tsx` | 글 목록과 이력 화면 |
| `fe/app/admin-layout.tsx` | 메뉴 항목 |

**순수 규칙의 자리**: 지금 구조에는 순수 도메인 객체만 모으는 모듈이 없다. 그래서 스펙이 엔티티 곁에 두라고 한 것(주소 값 타입, 등록 기한 규칙, 직전 판 찾기)은 db-core 에, 수집 잡만 쓰는 것(글자 뽑기, 유형 판정, 줄 수 세기, 알림 문구)은 core-batch `domain/news` 에 둔다. core-domain 모듈이 들어오면 `BrawlStarsNewsArticleType`, `BrawlStarsPatchRegistrationPolicy`, `…batch/domain/news/*` 가 옮길 대상이다. 엔티티에 묶인 `BrawlStarsNewsArticleLink`(임베디드)와 `BrawlStarsNewsArticlePreviousFinder`(리포지토리 사용)는 db-core 에 남는다.

---

### Task 1: 용어·도메인 정의와 글 데이터 모델

**Files:**
- Modify: `docs/superglossary/glossary.md`, `docs/superdomain/DOMAIN.md`
- Create: 위 표의 core-enum 파일, `…db/brawlstars/news/` 의 네 파일
- Modify: `schema.sql`, Create: `db/migration/V{yyMMdd}a__add_brawlstars_news_article_table.sql`
- Test: `…db/brawlstars/news/BrawlStarsNewsArticleLinkTest.kt`, `…db/brawlstars/news/repository/BrawlStarsNewsArticlePreviousFinderTest.kt`

**Interfaces:**
- Produces:

```kotlin
enum class BrawlStarsNewsArticleType { RELEASE_NOTE, OTHER }

@Embeddable
data class BrawlStarsNewsArticleLink(
    @Column(name = "link_url", updatable = false, nullable = false, length = 500) val linkUrl: String,
    @Column(name = "article_key", updatable = false, nullable = false, length = 255) val articleKey: String,
) {
    companion object {
        const val SITE_URL = "https://supercell.com"
        /** 목록이 주는 경로(`/en/games/brawlstars/blog/{분류}/{슬러그}`, 끝 슬래시는 있어도 된다)로 만든다. */
        fun of(path: String): BrawlStarsNewsArticleLink
    }
}

class BrawlStarsNewsArticleEntity(
    link: BrawlStarsNewsArticleLink, title: String, publishedAt: Instant,
    type: BrawlStarsNewsArticleType, rawBody: String, content: String,
) : BaseTimeEntity() {
    val id: Long?                         // brawlstars_news_article_id, IDENTITY
    fun changeType(type: BrawlStarsNewsArticleType)
    fun changeContent(content: String)
}

interface BrawlStarsNewsArticleJpaRepository : JpaRepository<BrawlStarsNewsArticleEntity, Long> {
    fun findTopByLinkLinkUrlAndIdLessThanOrderByIdDesc(linkUrl: String, id: Long): BrawlStarsNewsArticleEntity?
    fun findTopByLinkArticleKeyAndIdLessThanOrderByIdDesc(articleKey: String, id: Long): BrawlStarsNewsArticleEntity?
    fun findAllByLinkArticleKeyOrderByIdAsc(articleKey: String): List<BrawlStarsNewsArticleEntity>
}

/** 같은 주소의 가장 최근 행을 먼저 찾고, 없으면 같은 주소 키의 가장 최근 행이다. 가장 최근은 id 가 가장 큰 행이다. */
fun BrawlStarsNewsArticleJpaRepository.findPrevious(
    link: BrawlStarsNewsArticleLink,
    beforeId: Long = Long.MAX_VALUE,
): BrawlStarsNewsArticleEntity?
```

- [ ] **Step 1: 용어를 등록한다.** `/superglossary:glossary` 로 사전에 없는 단어를 넣는다: 뉴스 news, 글 article, 블로그 blog, 링크 link, 제목 title, 게시 publish, 원문 raw, 본문 body, 메모 memo, 릴리스 release, 노트 note, 기타 other, 기한 deadline, 이전 previous, 추출 extract, 요약 summary. 이미 있는 `버전 | version` 의 설명에 "글의 판(versions)"을 더한다.
- [ ] **Step 2: 도메인 정의를 고친다.** `/superdomain:domain` 으로 `docs/superdomain/DOMAIN.md` 의 brawlstars 에 하위 도메인 둘을 더한다: `news — 공식 블로그의 글과 그 변경 이력`, `patch — 운영자가 등록하는 패치일`. "외부"에 공식 블로그를 더한다. 클래스 이름과 구현 세부는 적지 않는다.
- [ ] **Step 3: 주소 값 타입의 실패하는 테스트를 쓴다.**

```kotlin
// BrawlStarsNewsArticleLinkTest (단위)
@Test fun `목록의 경로에 사이트 주소를 붙이고 끝 슬래시를 뗀다`() {
    val link = BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/game-updates/release-notes-november-2026/")
    assertThat(link.linkUrl).isEqualTo("https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026")
}
@ParameterizedTest @CsvSource(
    "update65-2, update65", "update65-5, update65", "brawl-cup-event-guide-2, brawl-cup-event-guide",
    "release-notes-november-2026, release-notes-november-2026", "balance-changes-13-2, balance-changes-13",
    "update65-2-3, update65-2", "patch-notes, patch-notes",
)
fun `마지막 구간에서 끝의 숫자 한두 자리를 한 번만 뗀다`(slug: String, key: String) {
    assertThat(BrawlStarsNewsArticleLink.of("/en/games/brawlstars/blog/game-updates/$slug").articleKey).isEqualTo(key)
}
```

- [ ] **Step 4: 실패를 확인한다.** Run: `./gradlew :infrastructure:db-core:test --tests "*BrawlStarsNewsArticleLinkTest"` → 컴파일 실패.
- [ ] **Step 5: enum, 값 타입, 엔티티, 리포지토리를 만든다.** 엔티티의 `rawBody` 와 `content` 는 `columnDefinition = "text"`, `type` 은 `@Enumerated(EnumType.STRING)` 에 `length = 25`, `title` 은 `length = 255` 다. `type` 과 `content` 만 바꿀 수 있고 나머지는 `updatable = false` 다. `@Table` 에 인덱스와 유니크를 두지 않는다(스펙 §4.1). KDoc 에 "한 행이 한 판이고 `type`·`content` 말고는 바꾸지 않는다"를 적는다.
- [ ] **Step 6: 직전 판 찾기의 실패하는 테스트를 쓴다.** `DbCoreContextTest` 를 상속한다.

```kotlin
// BrawlStarsNewsArticlePreviousFinderTest
@Test fun `같은 주소의 행이 있으면 주소 키가 같은 더 최근 행보다 그것을 고른다`()
    // update65-2(id=1), update65-3(id=2) 저장 → findPrevious(of(".../update65-2")) 는 id=1
@Test fun `같은 주소의 행이 없으면 같은 주소 키의 가장 최근 행을 고른다`()
    // update65-2(id=1) 저장 → findPrevious(of(".../update65-3")) 는 id=1
@Test fun `beforeId 보다 앞선 행만 본다`()
    // 같은 주소로 id=1, id=2 저장 → findPrevious(link, beforeId = 2) 는 id=1, beforeId = 1 이면 null
@Test fun `아무 행도 없으면 null 이다`()
```

- [ ] **Step 7: `findPrevious` 를 구현하고 테스트를 통과시킨다.** Run: `./gradlew :infrastructure:db-core:test --tests "*BrawlStarsNewsArticle*"` → PASS.
- [ ] **Step 8: DDL 을 쓴다.** `schema.sql` 의 `brawlstars_api_spec` 정의 뒤에 넣고, 같은 문장을 마이그레이션 파일에 넣는다.

```sql
-- brawlstars_news_article table
create table if not exists brawlstars_news_article
(
    brawlstars_news_article_id bigint generated always as identity primary key,
    article_key                varchar(255) not null,
    link_url                   varchar(500) not null,
    title                      varchar(255) not null,
    published_at               timestamptz  not null,
    type                       varchar(25)  not null,
    raw_body                   text         not null,
    content                    text         not null,
    created_at                 timestamptz  not null default now(),
    updated_at                 timestamptz  not null default now()
);
```

- [ ] **Step 9: 커밋한다.** `feat(backend): 공식 뉴스 글의 데이터 모델을 추가한다`

### Task 2: 패치일 데이터 모델과 등록 기한 규칙

**Files:**
- Create: `…db/brawlstars/patch/` 의 세 파일
- Modify: `schema.sql`, Create: `db/migration/V{yyMMdd}b__add_brawlstars_patch_table.sql`
- Test: `…db/brawlstars/patch/BrawlStarsPatchRegistrationPolicyTest.kt`, `…db/brawlstars/patch/repository/BrawlStarsPatchJpaRepositoryTest.kt`

**Interfaces:**
- Produces:

```kotlin
class BrawlStarsPatchEntity(patchDate: LocalDate, memo: String?) : BaseTimeEntity() {
    val id: Long?                         // brawlstars_patch_id, IDENTITY
    fun update(patchDate: LocalDate, memo: String?)
}

object BrawlStarsPatchRegistrationPolicy {
    const val REGISTRATION_DEADLINE_DAYS = 30L
    /** 오늘에서 패치일까지가 0일 이상 30일 이하일 때만 참이다. 미래 날짜는 거짓이다. */
    fun isRegistrable(today: LocalDate, patchDate: LocalDate): Boolean
    fun earliestRegistrableDate(today: LocalDate): LocalDate     // today - 30일
}

interface BrawlStarsPatchJpaRepository : JpaRepository<BrawlStarsPatchEntity, Long> {
    fun findAllByOrderByPatchDateDesc(): List<BrawlStarsPatchEntity>
    fun existsByPatchDate(patchDate: LocalDate): Boolean
}
```

- [ ] **Step 1: 기한 규칙의 실패하는 테스트를 쓴다.**

```kotlin
// BrawlStarsPatchRegistrationPolicyTest (단위), today = 2026-12-01
@ParameterizedTest @CsvSource(
    "2026-12-01, true", "2026-11-01, true", "2026-10-31, false", "2026-12-02, false",
)
fun `오늘에서 0일 이상 30일 이하인 날짜만 등록할 수 있다`(patchDate: LocalDate, expected: Boolean)
@Test fun `등록할 수 있는 가장 이른 날짜는 30일 전이다`()   // 2026-11-01
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** Run: `./gradlew :infrastructure:db-core:test --tests "*BrawlStarsPatchRegistrationPolicyTest"` → 컴파일 실패 → 구현 → PASS. KDoc 에 "두 번째 스펙은 이 기한 안의 날짜를 원본 전투에서 다시 읽는다. 원본 전투 보관 일수가 이 값보다 길어야 한다"를 적는다.
- [ ] **Step 3: 엔티티와 리포지토리의 실패하는 테스트를 쓴다.** `DbCoreContextTest` 를 상속한다.

```kotlin
// BrawlStarsPatchJpaRepositoryTest
@Test fun `같은 패치일을 두 번 저장하면 실패한다`()            // DataIntegrityViolationException
@Test fun `패치일이 늦은 것부터 돌려준다`()
@Test fun `패치일과 메모를 고친다`()
```

- [ ] **Step 4: 엔티티와 리포지토리를 만든다.** `@Table(name = "brawlstars_patch", indexes = [Index(name = "uk_brawlstars_patch__patch_date", columnList = "patch_date", unique = true)])`. `memo` 는 `length = 255`, nullable 이다. Run: `./gradlew :infrastructure:db-core:test --tests "*BrawlStarsPatch*"` → PASS.
- [ ] **Step 5: DDL 과 초기값을 쓴다.** 테이블과 유니크 인덱스는 `schema.sql` 과 마이그레이션 양쪽에, `insert` 는 마이그레이션에만 넣는다. 마이그레이션 머리 주석에 "멱등: 같은 날짜가 있으면 넘어간다"를 적는다.

```sql
-- brawlstars_patch table
create table if not exists brawlstars_patch
(
    brawlstars_patch_id bigint generated always as identity primary key,
    patch_date          date         not null,
    memo                varchar(255) null,
    created_at          timestamptz  not null default now(),
    updated_at          timestamptz  not null default now()
);
create unique index if not exists uk_brawlstars_patch__patch_date on brawlstars_patch (patch_date);

-- 마이그레이션 파일에만
insert into brawlstars_patch (patch_date)
values ('2026-11-03'), ('2026-11-11')
on conflict (patch_date) do nothing;
```

- [ ] **Step 6: 커밋한다.** `feat(backend): 패치일의 데이터 모델과 등록 기한 규칙을 추가한다`

### Task 3: 글자 뽑기

**Files:**
- Create: `…batch/domain/news/NewsArticleContentExtractor.kt`
- Move: `docs/superpowers/plans/fixtures/brawlstars-news/article-release-notes-november-2026.masked.html` → `imstargg-backend/core/core-batch/src/test/resources/brawlstars-blog/article-release-notes-november-2026.masked.html`
- Test: `…batch/domain/news/NewsArticleContentExtractorTest.kt`

**Interfaces:**
- Produces: `object NewsArticleContentExtractor { fun extract(rawBody: String): String }`. `rawBody` 는 `bodyCollection` 배열의 JSON 문자열이고, 결과는 줄을 `\n` 으로 이은 문자열이다(끝에 줄바꿈 없음).

**규칙.** 스펙 §6 을 사이트의 실제 JSON 이름으로 구체화한 것이다. Jackson 3(`tools.jackson.databind.json.JsonMapper`)의 트리로 읽는다.

1. 블록은 `__typename` 으로 가른다.
   - `TextBlock`: `title` 을 한 줄로 쓰고 `text.json` 을 이어 쓴다.
   - `FeatureBlock`: `title` 을 한 줄로 쓰고 `featureText.json` 을 이어 쓴다.
   - `ImageBlock`, `VideoBlock`: 버린다.
   - 그 밖: `[?{__typename}]` 한 줄.
2. 본문 나무는 문서 순서대로 내려간다. 노드는 `nodeType` 으로 가른다.
   - `paragraph`, `heading-1` 부터 `heading-6` 까지: 그 아래 모든 `text` 노드의 `value` 를 구분자 없이 이어 붙인 뒤, 줄바꿈(`\r\n`, `\r`, `\n`)에서 줄을 나눈다. 문단 안의 줄바꿈은 화면에서 줄이 바뀌는 자리다.
   - `table-row`: 칸마다 그 아래 `text` 의 `value` 를 이어 붙이고(줄바꿈은 공백으로), 칸을 공백 하나로 이어 한 줄로 쓴다. 그 아래로 더 내려가지 않는다.
   - 그 밖(`document`, `unordered-list`, `ordered-list`, `list-item`, `table`, `hr`, `hyperlink` 등): `content` 로 내려간다. 목록 항목은 그 안의 문단이 한 줄이 된다. 링크·굵게·기울임은 `text` 의 `value` 만 남는다.
3. 줄마다 다듬는다. 특수 공백(`U+00A0`, `U+1680`, `U+2000` 부터 `U+200B` 까지, `U+202F`, `U+205F`, `U+3000`, `U+FEFF`, 탭)을 일반 공백으로 바꾸고, 연속 공백을 한 칸으로 줄이고, 앞뒤 공백을 떼고, 빈 줄을 버린다.

- [ ] **Step 1: 픽스처를 옮긴다.** `git mv` 로 옮긴다. 이 파일은 2026-11-11 의 11월 릴리스 노트에서 글자를 `x`, 숫자를 `0` 으로 바꾼 사본이다. 구조, 공백, 문장 부호, 이모지는 그대로다.
- [ ] **Step 2: 실패하는 테스트를 쓴다.**

```kotlin
// NewsArticleContentExtractorTest (단위)
@Test fun `글 블록은 제목 한 줄 뒤에 문단과 소제목과 목록 항목을 한 줄씩 쓴다`()
@Test fun `기능 소개 블록은 제목 한 줄 뒤에 본문을 쓴다`()                 // FeatureBlock.featureText
@Test fun `이미지와 영상 블록은 버린다`()                                   // ImageBlock, VideoBlock
@Test fun `모르는 블록은 종류를 적은 한 줄을 남긴다`()                      // {"__typename":"QuizBlock"} → "[?QuizBlock]"
@Test fun `표의 행은 칸을 공백으로 이어 한 줄로 쓴다`()                     // 칸 "A", "B" → "A B"
@Test fun `링크와 굵게와 기울임은 글자만 남긴다`()                          // hyperlink + marks → 이어진 한 줄
@Test fun `문단 안의 줄바꿈에서 줄을 나눈다`()                              // "a\nb" → "a", "b"
@Test fun `특수 공백은 공백으로 바꾸고 연속 공백은 줄이고 빈 줄은 버린다`() // "a  b", "   " → "a b"
@Test fun `실제 글의 사본에서 뽑은 결과가 고정돼 있다`() {
    val html = ClassPathResource("brawlstars-blog/article-release-notes-november-2026.masked.html").getContentAsString(Charsets.UTF_8)
    val rawBody = bodyCollectionOf(html)   // __NEXT_DATA__ 의 props.pageProps.bodyCollection 을 JSON 문자열로 꺼내는 테스트 도우미
    val content = NewsArticleContentExtractor.extract(rawBody)
    val lines = content.lines()
    assertThat(lines).hasSize(635)
    assertThat(lines[0]).isEqualTo("xxxxxxxxxxx - xxxxxxxx 00")
    assertThat(lines[2]).isEqualTo("⚖️ xxxxxxx xxxxxxx ⚖️")
    assertThat(lines.last()).isEqualTo("xxxxx xxxx xxxxxx xxx xxxxxxx xxx xxxxxxx xx xxxx xxxxx xxxxxxx xxxxx.")
    assertThat(content.length).isEqualTo(43222)
    assertThat(lines).noneMatch { it.isBlank() || it.contains("  ") || it != it.trim() }
}
```

- [ ] **Step 3: 실패를 확인한다.** Run: `./gradlew :core:core-batch:test --tests "*NewsArticleContentExtractorTest"` → 컴파일 실패.
- [ ] **Step 4: 구현하고 통과시킨다.** 635줄과 43,222자는 위 규칙을 따로 구현해 낸 값이다. 수가 다르면 규칙 2·3 을 다시 본다.
- [ ] **Step 5: 커밋한다.** `feat(backend): 공식 뉴스 본문에서 줄별 글자를 뽑는다`

### Task 4: 블로그 읽기 클라이언트

**Files:**
- Create: `…batch/support/blog/BrawlStarsBlogClient.kt`, `BrawlStarsBlogArticleSummary.kt`, `BrawlStarsBlogPageException.kt`
- Move: `docs/superpowers/plans/fixtures/brawlstars-news/list-page-1.html` → `imstargg-backend/core/core-batch/src/test/resources/brawlstars-blog/list-page-1.html`
- Test: `…batch/support/blog/BrawlStarsBlogClientTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsNewsArticleLink` (Task 1).
- Produces:

```kotlin
data class BrawlStarsBlogArticleSummary(val title: String, val link: BrawlStarsNewsArticleLink, val publishedAt: Instant)

class BrawlStarsBlogPageException(message: String) : RuntimeException(message)

@Component
class BrawlStarsBlogClient(restClientBuilder: RestClient.Builder) {
    /** 목록의 [page] 쪽(1부터)을 읽는다. 제목은 앞뒤 공백을 뗀다. */
    fun getArticleSummaries(page: Int): List<BrawlStarsBlogArticleSummary>
    /** 글 페이지의 `bodyCollection` 을 JSON 문자열 그대로 돌려준다. */
    fun getRawBody(link: BrawlStarsNewsArticleLink): String
}
```

- 요청 주소: 1쪽 `https://supercell.com/en/games/brawlstars/blog/`, N쪽 `https://supercell.com/en/games/brawlstars/blog/page/{N}/`, 글은 `link.linkUrl + "/"`.
- `RestClient` 는 주입받은 `RestClient.Builder` 로 클래스 안에서 만든다. 빈으로 등록하지 않는다(`coreApiRestClient` 와 주입이 겹친다). 주입받은 빌더에는 공통 HTTP 설정(연결 3초, 응답 10초, 재시도 없음)이 걸려 있다.
- 응답은 `ByteArray` 로 받아 UTF-8 로 해석한다. `<script id="__NEXT_DATA__" …>` 의 내용을 꺼내 Jackson 3 로 읽는다.
- 검증(스펙 §5.3). 하나라도 어긋나면 `BrawlStarsBlogPageException` 이다: `__NEXT_DATA__` 가 있다. 목록은 `props.pageProps.articles` 가 비어 있지 않은 배열이다. 글은 `page` 가 `/en/games/brawlstars/blog/[category]/[title]` 이고 `props.pageProps.bodyCollection` 이 비어 있지 않은 배열이다. 200 이 아닌 응답은 `RestClient` 가 던지는 예외를 그대로 둔다.
- `publishDate` 는 `2026-11-11T05:00:00.000+02:00` 과 `2026-10-23T08:16:49.217Z` 두 꼴이 섞여 온다. `OffsetDateTime.parse(…).toInstant()` 로 읽는다.

- [ ] **Step 1: 픽스처를 옮긴다.** `git mv`. 2026-11-11 의 목록 1쪽에서 글 6건의 제목, 주소, 게시일만 남긴 파일이다. 옮긴 뒤 `docs/superpowers/plans/fixtures/` 가 비면 디렉터리가 사라진다.
- [ ] **Step 2: 실패하는 테스트를 쓴다.** `RestClient.builder()` 에 `MockRestServiceServer.bindTo(builder)` 를 걸어 클라이언트를 직접 만든다(본보기: `…batch/support/coreapi/CoreApiClientTest.kt`).

```kotlin
// BrawlStarsBlogClientTest (단위)
@Test fun `목록 1쪽을 읽는다`() {
    // GET https://supercell.com/en/games/brawlstars/blog/ → list-page-1.html
    assertThat(summaries).hasSize(6)
    assertThat(summaries[0].title).isEqualTo("Release Notes November 2026")
    assertThat(summaries[0].link.linkUrl).isEqualTo("https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026")
    assertThat(summaries[0].publishedAt).isEqualTo(Instant.parse("2026-11-11T03:00:00Z"))
    assertThat(summaries[1].publishedAt).isEqualTo(Instant.parse("2026-10-23T08:16:49.217Z"))
}
@Test fun `목록 2쪽은 page 경로로 요청한다`()           // GET …/blog/page/2/
@Test fun `글 본문을 JSON 문자열로 돌려준다`()            // GET linkUrl + "/" → 배열이고 블록이 10개
@Test fun `문자 인코딩 표시가 없어도 한글과 이모지를 읽는다`() // Content-Type: text/html, 본문에 "한글 ⚖️" 가 든 TextBlock
@Test fun `NEXT_DATA 가 없으면 예외다`()
@Test fun `목록에 글이 없으면 예외다`()                   // articles: []
@Test fun `없는 글의 페이지는 예외다`()                   // 200 이고 "page":"/404"
@Test fun `글 페이지에 본문이 없으면 예외다`()            // bodyCollection: []
@Test fun `서버 오류는 그대로 던진다`()                   // 500 → HttpServerErrorException
```

- [ ] **Step 3: 실패를 확인하고 구현한다.** Run: `./gradlew :core:core-batch:test --tests "*BrawlStarsBlogClientTest"` → 컴파일 실패 → 구현 → PASS.
- [ ] **Step 4: 커밋한다.** `feat(backend): 공식 블로그의 목록과 글을 읽는 클라이언트를 추가한다`

### Task 5: 알림 발송기가 전송 실패를 던진다

**Files:**
- Modify: `…batch/support/alert/DiscordAlertManager.kt`, `…batch/support/alert/AlertConfig.kt`
- Modify(KDoc 만): `…batch/job/statistics/battleresult/BrawlerBattleResultStatsTask.kt`, `…batch/job/statistics/battlerank/BrawlerBattleRankStatsTask.kt` 의 "`DiscordAlertManager` 는 전송 실패를 삼키지만" 문장
- Test: `…batch/support/alert/DiscordAlertManagerTest.kt`, `…batch/support/alert/AlertConfigTest.kt`

**Interfaces:**
- Produces: `AlertManager.alert(alertCommand)` 가 전송 실패 시 예외를 던진다. 시그니처는 그대로다.

**동작**(스펙 §8.1):
- 실패하면 ERROR 로그에 알림 문구 전문(`DiscordWebhookRequest.from(alertCommand)` 의 내용)을 남긴다.
- 상태 코드 오류(`RestClientResponseException`)는 받은 예외를 그대로 던진다. Discord 의 503 이 `HttpServerErrorException.ServiceUnavailable` 로 나가야 `BrawlStarsApiTasklet` 을 쓰는 잡의 동작이 스펙대로다.
- 연결 오류(`ResourceAccessException`)는 예외 메시지에 요청 주소가 들어간다. 웹훅 주소는 비밀이므로 주소를 뺀 `IllegalStateException("Discord 알림 전송 실패: ${원인 예외의 클래스 이름}")` 으로 바꿔 던지고, 로그에도 원래 예외를 싣지 않는다.
- `AlertConfig` 는 정적 `RestClient.builder()` 대신 빈으로 주입받은 `RestClient.Builder` 를 넘긴다. 공통 HTTP 설정의 타임아웃이 걸린다.

- [ ] **Step 1: 발송기의 실패하는 테스트를 쓴다.** 웹훅 주소는 `https://discord.test/api/webhooks/1/secret-token` 을 쓴다.

```kotlin
// DiscordAlertManagerTest (단위, MockRestServiceServer)
@Test fun `전송에 성공하면 예외가 없다`()
@Test fun `4xx 응답이면 받은 예외를 그대로 던진다`()        // isInstanceOf(HttpClientErrorException::class.java), 스택 문자열에 "secret-token" 없음
@Test fun `503 응답이면 ServiceUnavailable 예외를 그대로 던진다`() // isInstanceOf(HttpServerErrorException.ServiceUnavailable::class.java)
@Test fun `연결 오류의 예외에는 웹훅 주소가 없다`() {
    // withException(IOException("connect timed out"))
    assertThatThrownBy { sut.alert(command) }
        .isInstanceOf(IllegalStateException::class.java)
        .satisfies({ ex -> assertThat(ex.stackTraceToString()).doesNotContain("secret-token") })
}
```

- [ ] **Step 2: 설정의 실패하는 테스트를 쓴다.** `ApplicationContextRunner` 를 쓴다(본보기: `HttpClientYmlTest`).

```kotlin
// AlertConfigTest
@Test fun `설정이 없으면 아무것도 보내지 않는 발송기가 뜬다`()         // NoOpsAlertManager
@Test fun `Discord 설정이면 Discord 발송기가 뜬다`()                   // app.alert.type=DISCORD, app.alert.discord.webhook-url=…
@Test fun `Discord 발송기는 컨텍스트의 RestClient 빌더를 쓴다`()       // RestClientCustomizer 빈이 호출됐는지로 확인
```

- [ ] **Step 3: 실패를 확인하고 구현한다.** Run: `./gradlew :core:core-batch:test --tests "*DiscordAlertManagerTest" --tests "*AlertConfigTest"` → FAIL → 구현 → PASS.
- [ ] **Step 4: 기존 테스트를 돌린다.** Run: `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.support.alert.*"` → PASS. `JobFailureAlertListenerTest` 의 "알림 전송이 예외를 던져도 리스너는 예외를 밖으로 내지 않는다"가 그대로 통과해야 한다.
- [ ] **Step 5: 커밋한다.** `fix(backend): 알림 전송이 실패하면 예외를 던진다`

### Task 6: 유형 판정, 줄 수 세기, 알림 문구

**Files:**
- Create: `…batch/domain/news/NewsArticleTypeClassifier.kt`, `NewsArticleContentDiff.kt`, `NewsArticleAlertMessage.kt`
- Test: 같은 이름의 `…Test.kt` 세 개

**Interfaces:**
- Consumes: `BrawlStarsNewsArticleEntity`, `BrawlStarsNewsArticleType` (Task 1), `AlertCommand`.
- Produces:

```kotlin
object NewsArticleTypeClassifier { fun classify(title: String): BrawlStarsNewsArticleType }

/** 두 본문의 줄을 견줘 한쪽에만 있는 줄을 센다. 같은 줄이 여러 번 나오면 횟수의 차이만큼 센다. */
data class NewsArticleContentDiff(val addedLineCount: Int, val removedLineCount: Int) {
    companion object { fun of(previousContent: String, content: String): NewsArticleContentDiff }
}

object NewsArticleAlertMessage {
    fun newArticle(article: BrawlStarsNewsArticleEntity, detectedAt: Instant): AlertCommand
    fun changed(
        article: BrawlStarsNewsArticleEntity, previous: BrawlStarsNewsArticleEntity,
        previousContent: String, previousSeenAt: Instant, detectedAt: Instant,
    ): AlertCommand
    fun firstRunSummary(articles: List<BrawlStarsNewsArticleEntity>): AlertCommand
}
```

**문구**(스펙 §7). `AlertCommand(title, content, ex = null)` 로 만든다. 시각은 `yyyy-MM-dd HH:mm KST`, 게시일은 KST 날짜 `yyyy-MM-dd` 다. 유형은 `릴리스 노트`, `기타` 로 적는다. 본문은 싣지 않는다.

| 종류 | title | content 의 줄 |
|---|---|---|
| 새 글 | `[새 글] [{유형}] {제목}` | 주소 / `게시일 {날짜}` / `감지 {시각}` |
| 변경 | `[변경] [{유형}] {제목}` | 주소 / `감지 {시각} · 직전 판 {previousSeenAt}` / 바뀐 항목 |
| 첫 실행 요약 | `[첫 실행] 공식 뉴스 수집` | `글 {N}건 저장, 릴리스 노트 {M}건` / 가장 최근 릴리스 노트의 제목 / 그 주소. 릴리스 노트가 없으면 뒤 두 줄 대신 `릴리스 노트 없음` |

변경의 "바뀐 항목"은 바뀐 것만 한 줄씩 적는다: `주소 {이전} → {새}`, `제목 {이전} → {새}`, `게시일 {이전} → {새}`, 그리고 본문은 `본문 {a}줄 추가, {b}줄 삭제` 또는 `본문 동일`. `previousContent` 는 직전 판의 `raw_body` 를 지금 규칙으로 다시 뽑은 본문이고, `previousSeenAt` 은 직전 판의 `created_at` 이다. `created_at` 은 저장할 때 DB 가 채우는 값이라 단위 테스트에서 엔티티에 넣을 수 없어 인자로 받는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.**

```kotlin
// NewsArticleTypeClassifierTest
@ParameterizedTest @CsvSource(delimiter = '|', value = [
    "Release Notes November 2026|RELEASE_NOTE", "PATCH NOTES: Angels vs Demons|RELEASE_NOTE", "UPDATE 65: BUFFIES!|RELEASE_NOTE",
    "Brawl Stars x Duolingo|OTHER", "Balance Changes|OTHER", "Updates are coming|OTHER",
    "Update 2 the rules|RELEASE_NOTE",   // 릴리스 노트가 아니어도 "update + 숫자"면 잡힌다. 규칙이 넓다는 것을 남긴다.
])
fun `제목으로 유형을 판정한다`(title: String, expected: BrawlStarsNewsArticleType)
```

```kotlin
// NewsArticleContentDiffTest
@Test fun `한쪽에만 있는 줄을 센다`()            // "a\nb\nc" → "a\nc\nd\ne" 는 추가 2, 삭제 1
@Test fun `같은 줄이 여러 번이면 횟수의 차이만큼 센다`() // "a\na" → "a" 는 삭제 1
@Test fun `같으면 0 이다`()

// NewsArticleAlertMessageTest — 스펙 §7 의 예를 그대로 고정한다
@Test fun `변경 알림은 바뀐 것만 적는다`() {
    // 직전 판: 게시일 2026-11-03, previousSeenAt 2026-11-10T12:20:00Z / 새 판: 게시일 2026-11-11, 감지 2026-11-11T12:20:00Z, 본문 19줄 추가 4줄 삭제
    assertThat(command.title).isEqualTo("[변경] [릴리스 노트] Release Notes November 2026")
    assertThat(command.content).isEqualTo(
        """
        https://supercell.com/en/games/brawlstars/blog/game-updates/release-notes-november-2026
        감지 2026-11-11 21:20 KST · 직전 판 2026-11-10 21:20 KST
        게시일 2026-11-03 → 2026-11-11
        본문 19줄 추가, 4줄 삭제
        """.trimIndent(),
    )
}
@Test fun `주소만 바뀌면 주소와 본문 동일을 적는다`()
@Test fun `새 글 알림은 유형과 제목과 주소와 게시일과 감지 시각을 담는다`()
@Test fun `첫 실행 요약은 글 수와 릴리스 노트 수와 가장 최근 릴리스 노트를 담는다`()
@Test fun `첫 실행 요약은 릴리스 노트가 없으면 없다고 적는다`()
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** Run: `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.news.*"` → 컴파일 실패 → 구현 → PASS.
- [ ] **Step 3: 커밋한다.** `feat(backend): 공식 뉴스의 유형 판정과 알림 문구를 추가한다`

### Task 7: 수집 태스크

**Files:**
- Create: `…batch/job/brawlstars/task/BrawlStarsNewsCollectTask.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/resources/application.yml`, `imstargg-backend/core/core-batch/src/test/resources/application.yml`
- Test: `…batch/job/brawlstars/task/BrawlStarsNewsCollectTaskTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsBlogClient` (Task 4), `NewsArticleContentExtractor` (Task 3), `NewsArticleTypeClassifier`·`NewsArticleAlertMessage` (Task 6. `changed` 의 `previousSeenAt` 에는 직전 판의 `createdAt` 을 넘긴다), `BrawlStarsNewsArticleJpaRepository`·`findPrevious` (Task 1), `AlertManager`, `Clock`.
- Produces: `@Component class BrawlStarsNewsCollectTask(…) { fun collect() }`.

**설정**: `app.brawlstars-news.request-interval-min-millis`(기본 100), `app.brawlstars-news.request-interval-max-millis`(기본 1000). 테스트 yml 에서는 둘 다 0 이다. 요청마다 그 사이의 무작위 시간을 쉬고, 둘이 같으면 그 값만큼 쉰다(목록과 본문을 구분하지 않는다).

**흐름**(스펙 §5.2, §5.4, §5.5):
1. 목록 1쪽부터 3쪽까지 읽는다. 같은 `linkUrl` 이 두 쪽에 나오면 한 번만 다룬다.
2. 글 테이블이 비어 있으면 첫 실행이다. 모든 글의 본문을 받아 글자를 뽑고, 게시일이 이른 것부터 `saveAll` 한 번으로 저장한 뒤 요약 알림 1건을 보낸다. 유형은 제목으로 판정한다.
3. 아니면 게시일이 늦은 글부터 하나씩 처리한다.
   1. 본문을 받아 글자를 뽑는다.
   2. `findPrevious(link)` 로 직전 판을 찾는다.
   3. 직전 판이 있으면 그 `rawBody` 를 지금 규칙으로 다시 뽑는다. 저장된 `content` 와 다르면 같은 `articleKey` 의 모든 행의 `content` 를 각 행의 `rawBody` 로 다시 만들어 `saveAll` 로 저장한다. 알림은 없다.
   4. 직전 판이 있고 `linkUrl`, `title`, 다시 뽑은 본문이 모두 같으면 넘어간다.
   5. 아니면 새 행을 저장하고 이어서 알린다. 유형은 직전 판의 것을 물려받고, 직전 판이 없으면 제목으로 판정한다.
4. 예외는 잡지 않는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `CoreBatchContextTest` 를 상속하고 `@MockkBean BrawlStarsBlogClient`, `@MockkBean(relaxUnitFun = true) AlertManager` 를 둔다. `@AfterEach` 에서 `CleanUp.all()`. 목록은 1쪽에만 글을 주고 2·3쪽은 다른 글 하나씩을 준다.

```kotlin
// BrawlStarsNewsCollectTaskTest
@Test fun `첫 실행은 모든 글을 게시일이 이른 순서로 저장하고 요약 알림 하나만 보낸다`()
@Test fun `첫 실행이 중간에 실패하면 한 건도 남지 않는다`()                  // 세 번째 글의 getRawBody 가 예외 → count() == 0
@Test fun `변경이 없으면 저장도 알림도 없다`()
@Test fun `새 글은 저장하고 새 글 알림을 보낸다`()
@Test fun `본문이 바뀌면 새 판을 저장하고 변경 알림을 보낸다`()
@Test fun `제목이 바뀌면 새 판을 저장한다`()
@Test fun `주소가 바뀌면 같은 주소 키의 직전 판과 견줘 새 판을 저장한다`()   // update65-2 → update65-3, 본문 동일
@Test fun `새 판은 직전 판의 유형을 물려받는다`()                            // 직전 판을 OTHER 로 고쳐 둔 릴리스 노트 제목
@Test fun `같은 주소가 두 쪽에 나오면 한 번만 다룬다`()
@Test fun `같은 주소 키의 두 주소가 함께 있어도 두 번째 실행은 조용하다`()    // update65-2, update65-3 이 함께 있는 목록으로 두 번 실행
@Test fun `규칙이 바뀌면 저장된 본문만 고치고 알리지 않는다`()               // content 를 일부러 다르게 저장해 둔 판
@Test fun `글 하나가 실패하면 예외가 나고 그 앞의 저장은 남는다`()            // 게시일이 늦은 글은 저장됨
@Test fun `알림 전송이 실패하면 예외가 나고 저장은 남는다`()
@Test fun `실패한 실행 뒤에 다시 돌리면 남은 글만 저장하고 알린다`()
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** Run: `./gradlew :core:core-batch:test --tests "*BrawlStarsNewsCollectTaskTest"` → 컴파일 실패 → 구현 → PASS. 태스클릿 스텝에는 트랜잭션 매니저를 넘기지 않으므로 리포지토리 호출 하나가 트랜잭션 하나다. 첫 실행의 "전부 아니면 전무"는 `saveAll` 한 번으로 얻는다.
- [ ] **Step 3: 커밋한다.** `feat(backend): 공식 뉴스를 판 단위로 저장하고 알린다`

### Task 8: 잡 설정

**Files:**
- Create: `…batch/job/brawlstars/BrawlStarsNewsCollectJobConfig.kt`
- Test: `…batch/job/brawlstars/BrawlStarsNewsCollectJobTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsNewsCollectTask.collect()` (Task 7).
- Produces: `const val JOB_NAME = "brawlStarsNewsCollectJob"`, `const val STEP_NAME = "brawlStarsNewsCollectStep"`.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** 본보기는 `…batch/job/brawlstars/BattleEventSyncJobTest.kt` 다.

```kotlin
// BrawlStarsNewsCollectJobTest
@Test fun `잡이 수집을 한 번 부르고 끝난다`()                 // COMPLETED, verify(exactly = 1) { task.collect() }
@Test fun `파라미터 없이 두 번 실행할 수 있다`()               // 두 실행 모두 COMPLETED
@Test fun `수집이 예외를 던지면 잡이 실패한다`()               // FAILED. 503 이어도 FAILED 다(BrawlStarsApiTasklet 을 쓰지 않는다)
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** 스텝은 람다 태스클릿(`{ _, _ -> task.collect(); RepeatStatus.FINISHED }`)이고 잡에는 `.incrementer(RunIdIncrementer())` 를 붙인다. incrementer 가 없으면 파라미터 없는 잡의 두 번째 실행이 거부된다. Run: `./gradlew :core:core-batch:test --tests "*BrawlStarsNewsCollectJobTest" --tests "*JobParametersIncrementerGuardTest"` → PASS.
- [ ] **Step 3: core-batch 전체를 돌린다.** Run: `./gradlew :core:core-batch:test :core:core-batch:ktlintCheck` → PASS.
- [ ] **Step 4: 커밋한다.** `feat(backend): 공식 뉴스 수집 잡을 추가한다`

### Task 9: core-admin 패치일 API

**Files:**
- Create: `…admin/controller/brawlstars/AdminBrawlStarsPatchController.kt`, `…admin/application/brawlstars/AdminBrawlStarsPatchService.kt`, `…admin/request/brawlstars/BrawlStarsPatchRegisterRequest.kt`, `BrawlStarsPatchUpdateRequest.kt`, `…admin/response/brawlstars/BrawlStarsPatchResponse.kt`, `BrawlStarsPatchListResponse.kt`
- Test: `…admin/controller/brawlstars/AdminBrawlStarsPatchControllerTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsPatchEntity`, `BrawlStarsPatchJpaRepository`, `BrawlStarsPatchRegistrationPolicy` (Task 2), `Clock`.
- Produces(스펙 §9.1, §9.3):

| 메서드·경로 | 요청 | 응답 |
|---|---|---|
| `GET /patches` | | 200 `{ "today": "2026-12-01", "earliestRegistrableDate": "2026-11-01", "patches": [{ "id", "patchDate", "memo" }] }`. 패치일이 늦은 것부터 |
| `POST /patches` | `{ "patchDate": "2026-11-25", "memo": "…" }` | 200 `{ "id", "patchDate", "memo" }` |
| `PUT /patches/{id}` | `{ "patchDate", "memo" }` | 204 |
| `DELETE /patches/{id}` | | 204 |

- 오늘은 `LocalDate.now(clock)` 이다(`Clock` 은 KST).
- 400(`ResponseStatusException(HttpStatus.BAD_REQUEST, …)`): 등록하는 날짜가 기한 밖이거나 미래. 수정에서 날짜가 바뀌는데 옛 날짜나 새 날짜가 기한 밖. 삭제하는 패치일이 기한 밖. 같은 날짜가 이미 있음. 날짜 형식이 `yyyy-MM-dd` 가 아님.
- 수정에서 날짜가 그대로면 기한을 보지 않는다(메모만 고친다).
- 404: 없는 id.
- 통계를 다시 계산하지 않는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다.** `@AutoConfigureMockMvc` + `CoreAdminContextTest` 를 쓴다(본보기: `…admin/controller/operation/AdminFeedbackControllerTest.kt`). 시계는 `@TestConfiguration` 의 `@Primary` `Clock.fixed(Instant.parse("2026-12-01T03:00:00Z"), KOREA_ZONE)` 로 고정한다(본보기: core-api `FeedbackServiceTest` 의 `FixedClockConfig`).

```kotlin
// AdminBrawlStarsPatchControllerTest, 오늘 = 2026-12-01
@Test fun `목록은 오늘과 가장 이른 날짜와 패치일을 늦은 순서로 준다`()   // jsonPath("$.today") = "2026-12-01", "$.earliestRegistrableDate" = "2026-11-01"
@ParameterizedTest @CsvSource("2026-12-01, 200", "2026-11-01, 200", "2026-10-31, 400", "2026-12-02, 400")
fun `등록은 기한 안의 날짜만 받는다`(patchDate: String, status: Int)
@Test fun `같은 날짜를 두 번 등록하면 400 이다`()
@Test fun `날짜 형식이 틀리면 400 이다`()                                  // "2026/11/25"
@Test fun `날짜를 바꾸는 수정은 옛 날짜와 새 날짜가 모두 기한 안이어야 한다`()
@Test fun `옛 날짜가 기한 밖이면 메모만 고칠 수 있다`()                    // 같은 날짜 + 새 메모 → 204, 다른 날짜 → 400
@Test fun `이미 있는 날짜로 수정하면 400 이다`()
@Test fun `기한 밖의 패치일은 삭제할 수 없다`()
@Test fun `없는 id 의 수정과 삭제는 404 다`()
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** 서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰는 메서드에 `@Transactional` 을 단다. Run: `./gradlew :core:core-admin:test --tests "*AdminBrawlStarsPatchControllerTest"` → FAIL → 구현 → PASS.
- [ ] **Step 3: 커밋한다.** `feat(backend): 어드민에 패치일 API 를 추가한다`

### Task 10: core-admin 글 API

**Files:**
- Create: `…admin/controller/brawlstars/AdminBrawlStarsNewsArticleController.kt`, `…admin/application/brawlstars/AdminBrawlStarsNewsArticleService.kt`, `AdminBrawlStarsNewsArticleRepository.kt`, `AdminBrawlStarsNewsArticleSummary.kt`, `…admin/request/brawlstars/BrawlStarsNewsArticleTypeUpdateRequest.kt`, `…admin/response/brawlstars/BrawlStarsNewsArticleListItemResponse.kt`, `BrawlStarsNewsArticleDetailResponse.kt`, `BrawlStarsNewsArticleVersionResponse.kt`
- Test: `…admin/controller/brawlstars/AdminBrawlStarsNewsArticleControllerTest.kt`

**Interfaces:**
- Consumes: `BrawlStarsNewsArticleEntity`, `BrawlStarsNewsArticleJpaRepository`, `findPrevious` (Task 1).
- Produces(스펙 §9.1). `seenAt` 은 그 행의 `created_at` 이다.

| 메서드·경로 | 응답 |
|---|---|
| `GET /news-articles` | 200. 글(주소 키)마다 가장 최근 판 1건: `{ id, articleKey, linkUrl, title, type, publishedAt, seenAt, versionCount }`. 가장 최근 판의 id 가 큰 것부터. 본문은 없다 |
| `GET /news-articles/{id}` | 200 `{ id, articleKey, linkUrl, title, type, publishedAt, seenAt, content, previousId, previousSeenAt, previousContent }`. 직전 판이 없으면 `previous*` 는 null. 404 |
| `GET /news-articles/{id}/versions` | 200. 같은 주소 키의 모든 판, id 오름차순: `{ id, linkUrl, title, publishedAt, seenAt }`. 404 |
| `PUT /news-articles/{id}/type` | 요청 `{ "type": "RELEASE_NOTE" }`. 같은 주소 키의 모든 판에 적용. 204. 404. 모르는 유형은 400 |

- 목록과 판 목록은 `raw_body`, `content` 두 컬럼을 읽지 않는다(core-admin 의 DB 응답 대기가 3초다). `AdminBrawlStarsNewsArticleRepository` 가 QueryDSL 투영으로 읽는다(본보기: `…admin/application/operation/AdminFeedbackRepository.kt`).
- 상세의 직전 판은 `findPrevious(entity.link, beforeId = entity.id)` 다. 수집과 같은 함수를 쓴다.
- 원문 JSON(`raw_body`)은 어떤 응답에도 싣지 않는다.

```kotlin
data class AdminBrawlStarsNewsArticleSummary(
    val id: Long, val articleKey: String, val linkUrl: String, val title: String,
    val type: BrawlStarsNewsArticleType, val publishedAt: Instant, val seenAt: Instant, val versionCount: Long,
)

@Repository @Transactional(readOnly = true)
class AdminBrawlStarsNewsArticleRepository(entityManager: EntityManager) {
    fun findLatestVersions(): List<AdminBrawlStarsNewsArticleSummary>
    fun findVersions(articleKey: String): List<AdminBrawlStarsNewsArticleSummary>   // versionCount 는 쓰지 않는다
}
```

- [ ] **Step 1: 실패하는 테스트를 쓴다.**

```kotlin
// AdminBrawlStarsNewsArticleControllerTest
@Test fun `목록은 글마다 가장 최근 판 하나와 판 수를 준다`()          // 판이 둘인 글 + 하나인 글 → 2건, versionCount 2 와 1
@Test fun `목록에는 본문이 없다`()                                     // jsonPath("$[0].content").doesNotExist(), "$[0].rawBody".doesNotExist()
@Test fun `상세는 본문과 직전 판의 본문을 함께 준다`()
@Test fun `상세의 직전 판은 같은 주소의 판이 먼저다`()                 // update65-2(v1), update65-3(v1), update65-2(v2) → v2 의 previousId 는 update65-2(v1)
@Test fun `직전 판이 없으면 previous 는 null 이다`()
@Test fun `판 목록은 같은 주소 키의 판을 시간순으로 준다`()
@Test fun `유형 수정은 같은 글의 모든 판에 적용된다`()
@Test fun `모르는 유형은 400 이다`()
@Test fun `없는 id 는 404 다`()                                        // 상세, 판 목록, 유형 수정
```

- [ ] **Step 2: 실패를 확인하고 구현한다.** Run: `./gradlew :core:core-admin:test --tests "*AdminBrawlStarsNewsArticleControllerTest"` → FAIL → 구현 → PASS.
- [ ] **Step 3: 백엔드 전체를 돌린다.** Run: `./gradlew :core:core-admin:test :infrastructure:db-core:test :core:core-enum:test :core:core-batch:test ktlintCheck` → PASS.
- [ ] **Step 4: 커밋한다.** `feat(backend): 어드민에 공식 뉴스 글 API 를 추가한다`

### Task 11: admin-frontend 패치일 화면

**Files:**
- Modify: `fe/lib/types.ts`, `fe/app/admin-layout.tsx`
- Create: `fe/lib/api/brawlstars/patch.ts`, `fe/app/patches/page.tsx`, `fe/app/patches/_components/PatchFormDialog.tsx`

**Interfaces:**
- Consumes: Task 9 의 API.
- Produces:

```ts
// lib/types.ts
export interface BrawlStarsPatch { id: number; patchDate: string; memo: string | null }
export interface BrawlStarsPatchList { today: string; earliestRegistrableDate: string; patches: BrawlStarsPatch[] }

// lib/api/brawlstars/patch.ts
export async function getPatches(): Promise<BrawlStarsPatchList>
export async function registerPatch(data: { patchDate: string; memo: string | null }): Promise<BrawlStarsPatch>
export async function updatePatch(id: number, data: { patchDate: string; memo: string | null }): Promise<void>
export async function deletePatch(id: number): Promise<void>
```

**화면 규칙:**
- 로드는 `useCallback` + `useEffect` 에 `loading` 상태와 try/catch 를 쓴다(본보기: `fe/app/api-specs/[id]/page.tsx`). 변경 뒤에는 로드 함수를 다시 부른다. 성공은 `toast.success`, 실패는 `toast.error` 다.
- 목록 위에 "날짜는 KST 기준입니다"를 적는다. 표의 열은 패치일, 메모, 수정, 삭제다.
- 날짜 입력은 `<Input type="date" min={earliestRegistrableDate} max={today}>` 이고 값은 `yyyy-MM-dd` 문자열 그대로 주고받는다. `Date` 로 바꾸지 않는다(UTC 변환으로 날짜가 밀린다).
- 등록과 수정은 같은 `PatchFormDialog` 를 쓴다. 패치일이 `earliestRegistrableDate` 보다 이르면 수정 다이얼로그의 날짜 입력을 끄고 메모만 고치게 하며, 삭제 버튼도 끈다.
- 저장 전에 빈 날짜와 범위 밖 날짜를 막고 `toast.error` 를 띄운다.
- 삭제 확인은 점검 화면처럼 `Dialog` 로 한다(본보기: `fe/app/maintenances/page.tsx`). 트리거는 `<DialogTrigger render={<Button />}>` 꼴이다.
- 메뉴: `navItems` 에 `{ href: "/patches", label: "패치일" }` 을 더한다.

- [ ] **Step 1: 타입과 API 함수를 쓴다.** `request` 는 `fe/lib/api.ts` 의 것을 쓴다.
- [ ] **Step 2: 화면과 다이얼로그를 쓴다.**
- [ ] **Step 3: 검증한다.** Run: `pnpm lint && pnpm exec tsc --noEmit && pnpm build` → 오류 없음.
- [ ] **Step 4: 로컬에서 확인한다.** core-admin(`localhost:8080`)과 `pnpm dev` 를 띄우고 본다: 등록, 같은 날짜 다시 등록(실패 알림), 메모 수정, 날짜 수정, 삭제, 날짜 입력에서 31일 전과 내일이 골라지지 않는지. 확인한 것과 못 한 것을 보고에 적는다.
- [ ] **Step 5: 커밋한다.** `feat(admin-frontend): 패치일 화면을 추가한다`

### Task 12: admin-frontend 글 목록과 이력 화면

**Files:**
- Modify: `fe/lib/types.ts`, `fe/app/admin-layout.tsx`
- Create: `fe/lib/api/brawlstars/news-article.ts`, `fe/app/news-articles/page.tsx`, `fe/app/news-articles/[id]/page.tsx`, `fe/app/news-articles/_components/NewsArticleTypeSelect.tsx`, `NewsArticleDiffView.tsx`

**Interfaces:**
- Consumes: Task 10 의 API.
- Produces:

```ts
// lib/types.ts
export type NewsArticleType = "RELEASE_NOTE" | "OTHER"
export interface NewsArticleListItem { id: number; articleKey: string; linkUrl: string; title: string; type: NewsArticleType; publishedAt: string; seenAt: string; versionCount: number }
export interface NewsArticleDetail { id: number; articleKey: string; linkUrl: string; title: string; type: NewsArticleType; publishedAt: string; seenAt: string; content: string; previousId: number | null; previousSeenAt: string | null; previousContent: string | null }
export interface NewsArticleVersion { id: number; linkUrl: string; title: string; publishedAt: string; seenAt: string }

// lib/api/brawlstars/news-article.ts
export async function getNewsArticles(): Promise<NewsArticleListItem[]>
export async function getNewsArticleDetail(id: number): Promise<NewsArticleDetail>
export async function getNewsArticleVersions(id: number): Promise<NewsArticleVersion[]>
export async function updateNewsArticleType(id: number, type: NewsArticleType): Promise<void>
```

**화면 규칙:**
- **목록**(`/news-articles`): 글마다 한 줄. 열은 유형, 제목, 게시일, 처음 본 시각(`seenAt`), 판 수, 이력. 제목은 공식 글을 새 탭으로 연다(`target="_blank" rel="noopener noreferrer"`). 이력은 `/news-articles/{id}` 로 간다.
- **유형 바로 수정**: `NewsArticleTypeSelect({ id, initialType, onChanged })`. `Select` 로 고르면 바로 `updateNewsArticleType` 을 부른다. 먼저 값을 바꿔 보이고, 실패하면 되돌리고 `toast.error` 를 띄운다. 저장 중에는 끈다(본보기: `fe/app/feedbacks/_components/FeedbackCheckToggle.tsx`). `onValueChange` 에 null 이 올 수 있다. 표시는 `릴리스 노트`, `기타` 다.
- **이력**(`/news-articles/[id]`): 상세와 판 목록을 함께 읽는다. 왼쪽에 판 목록(처음 본 시각, 제목)을 시간순으로 보이고 지금 판을 강조한다. 다른 판을 고르면 `/news-articles/{그 id}` 로 옮긴다(쿼리 파라미터를 쓰지 않는다). 오른쪽에 고른 판과 직전 판의 줄 단위 비교를 보인다.
- **비교**: `NewsArticleDiffView({ previousContent, content })` 는 `fe/app/api-specs/_components/ApiSpecDiffView.tsx` 를 복제해 만든다(`diffLines`, 변경 없는 구간 접기). 직전 판이 없으면 "첫 판입니다"와 본문 전체를, 차이가 없으면 "직전 판과 본문이 같습니다"를 보인다. api-specs 의 컴포넌트는 고치지 않는다.
- 시각은 `new Date(iso).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })` 로 적는다.
- 메뉴: `navItems` 에 `{ href: "/news-articles", label: "공식 뉴스" }` 를 더한다.

- [ ] **Step 1: 타입과 API 함수를 쓴다.**
- [ ] **Step 2: 목록 화면과 유형 셀렉트를 쓴다.**
- [ ] **Step 3: 이력 화면과 비교 컴포넌트를 쓴다.**
- [ ] **Step 4: 검증한다.** Run: `pnpm lint && pnpm exec tsc --noEmit && pnpm build` → 오류 없음.
- [ ] **Step 5: 로컬에서 확인한다.** 로컬 DB 에 판이 둘인 글을 넣고 본다: 목록의 판 수, 유형 바꾸기와 새로 고침 뒤 유지, 이력에서 판 고르기, 추가·삭제 줄의 색, 첫 판의 안내. 확인한 것과 못 한 것을 보고에 적는다.
- [ ] **Step 6: 커밋한다.** `feat(admin-frontend): 공식 뉴스 글 목록과 이력 화면을 추가한다`

### Task 13: 마무리 검증

- [ ] **Step 1: 백엔드 전체 테스트.** Run: `./gradlew test ktlintCheck` → PASS.
- [ ] **Step 2: 용어 검사.** `/superglossary:check` 로 이 브랜치의 새 이름을 사전과 견준다. 위반이 있으면 고치거나 사전에 등록한다.
- [ ] **Step 3: 스펙 대조.** 스펙 §10 의 표를 한 줄씩 읽고 대응하는 테스트를 적는다. 빠진 줄이 있으면 테스트를 더한다.
- [ ] **Step 4: 사용하지 않게 된 것 확인.** `docs/superpowers/plans/fixtures/` 가 남아 있지 않은지 본다.

---

## 배포 (스펙 §11, 구현 PR 밖)

구현 PR 에 넣지 않는다. 순서가 바뀌면 옛 이미지가 모르는 잡 이름으로 뜬다.

1. 운영 DB 에 마이그레이션 두 파일을 직접 적용한다.
2. core-batch 를 배포하고 기존 잡이 정상인지 확인한다. 알림 발송기 변경이 이때 모든 잡에 적용된다.
3. 그 뒤에 `imstargg-deploy/apps/prod/core-batch/values.yaml` 의 `jobs:` 에 항목을 더한다.

```yaml
  - name: brawlstars-news-collect
    schedule: "20 0,12 * * *" # 09:20, 21:20 KST
    args:
      - "java -jar /app/application.jar --job.name=brawlStarsNewsCollectJob"
```

4. 첫 실행에서 글 18건이 저장되고 요약 알림이 오는지 확인한다.
