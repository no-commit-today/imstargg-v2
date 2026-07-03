# 관리자 하위 아이템 원스텝 등록 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** admin 에서 가젯/스타파워/하이퍼차지/버피 등록을 한 번의 사용자 액션(ID+이름+이미지)으로 통합하고, 브롤러 신규 등록 폼에서 하위 아이템까지 함께 등록할 수 있게 한다.

**Architecture:** core-admin 의 `BrawlerSubItemCreateRequest` 에 `names` 를 추가해 생성 트랜잭션에서 이름까지 저장(기존 `doUpdateMessages` 재사용). 프론트는 기존 "생성 API + 이미지 업로드 API 순차 호출" 오케스트레이션 패턴을 그대로 확장한다. 이미지 업로드 API 는 분리 유지.

**Tech Stack:** Kotlin + Spring Boot 4 (core-admin), Next.js App Router 클라이언트 컴포넌트 + shadcn + sonner (imstargg-admin-frontend)

**Spec:** `docs/superpowers/specs/2026-07-03-admin-subitem-onestep-registration-design.md`

## Global Constraints

- 저장소 루트: 이 worktree 루트. 백엔드 명령은 `imstargg-backend/`, 프론트 명령은 `imstargg-admin-frontend/` 에서 실행.
- 기존 엔드포인트의 경로·HTTP 메서드·상태코드 변경 금지. `PUT .../messages`, `PUT .../image` 는 수정 흐름용으로 그대로 유지.
- 백엔드 커밋 전: `./gradlew :core:core-admin:ktlintCheck` 통과. 테스트 컨벤션: 한글 백틱 메서드명, `sut`, `// given` `// when` `// then`, AssertJ, `CleanUp.all()`.
- 백엔드 context 테스트는 로컬 인프라 필요: `imstargg-backend/` 에서 `docker-compose up -d` (PostgreSQL 등) 후 실행.
- admin-frontend 는 테스트 프레임워크 없음 — 각 프론트 태스크의 검증은 `pnpm lint && pnpm exec tsc --noEmit && pnpm build` (CI 미포함, 로컬 검증 필수).
- admin-frontend 컨벤션: react-hook-form 미사용(제어 컴포넌트 + 수동 검증), 뮤테이션 성공 시 `toast.success` + reload / 실패 시 `toast.error` (sonner). `react-hooks/set-state-in-effect` 룰 주의 — useEffect 안에서 prop 파생 setState 금지.
- 커밋은 Conventional Commits. core-admin·admin-frontend 는 버전 bump 대상 아님.
- Kotlin: 인자 3개 이상 호출은 named arguments.

---

### Task 1: core-admin — 하위 아이템 생성 시 이름 함께 저장

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BrawlerSubItemCreateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BrawlerService.kt:187-209` (createStarPower/createGadget/createHyperCharge)
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BrawlerServiceTest.kt`

**Interfaces:**
- Consumes: 기존 `BrawlerService.doUpdateMessages(type, brawlStarsId, langMap)` private 헬퍼 (blank 값 필터링 포함, `BrawlerService.kt:324-334`), `MessageType.STARPOWER_NAME/GADGET_NAME/HYPERCHARGE_NAME`
- Produces: `POST /brawlers/{id}/star-powers|gadgets|hyper-charges` 요청 본문이 `{ "brawlStarsId": number, "names": { "KOREAN": string, "ENGLISH": string } }` 를 수용 (`names` 생략 가능 — 기본 빈 맵, 하위 호환). Task 2 가 이 계약을 사용.
- 컨트롤러(`BrawlerController.kt`)는 request 를 그대로 서비스에 넘기므로 **변경 없음**.

- [ ] **Step 1: 실패하는 테스트 작성**

`BrawlerServiceTest.kt` 를 다음과 같이 수정한다. 생성자에 `StarPowerJpaRepository`, `GadgetJpaRepository`, `HyperChargeJpaRepository` 주입을 추가하고, import 와 테스트 3개를 추가한다 (기존 테스트·구조는 그대로 유지).

```kotlin
package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BrawlerCreateRequest
import com.imstargg.core.admin.request.BrawlerSubItemCreateRequest
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrawlerServiceTest
    @Autowired
    constructor(
        private val sut: BrawlerService,
        private val brawlerJpaRepository: BrawlerJpaRepository,
        private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
        private val starPowerJpaRepository: StarPowerJpaRepository,
        private val gadgetJpaRepository: GadgetJpaRepository,
        private val hyperChargeJpaRepository: HyperChargeJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `브롤러 등록 시 이름과 기어가 함께 저장된다`() {
            // given
            val request =
                BrawlerCreateRequest(
                    brawlStarsId = 16000000L,
                    rarity = BrawlerRarity.MYTHIC,
                    role = BrawlerRole.DAMAGE_DEALER,
                    names = mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                    gearBrawlStarsIds = setOf(62000000L, 62000001L),
                )

            // when
            sut.createBrawler(request)

            // then
            assertThat(brawlerJpaRepository.findByBrawlStarsId(16000000L)).isNotNull
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, listOf(16000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                )
            val gearIds =
                brawlerGearJpaRepository.findAllByBrawlerBrawlStarsId(16000000L).map { it.gearBrawlStarsId }
            assertThat(gearIds).containsExactlyInAnyOrder(62000000L, 62000001L)
        }

        @Test
        fun `스타파워 등록 시 이름이 함께 저장된다`() {
            // given
            val request =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000100L,
                    names = mapOf(Language.KOREAN to "조개 충격", Language.ENGLISH to "Shell Shock"),
                )

            // when
            sut.createStarPower(brawlerBrawlStarsId = 16000000L, request = request)

            // then
            assertThat(starPowerJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, listOf(23000100L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "조개 충격", Language.ENGLISH to "Shell Shock"),
                )
        }

        @Test
        fun `가젯과 하이퍼차지 등록 시 이름이 함께 저장된다`() {
            // given
            val gadgetRequest =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000200L,
                    names = mapOf(Language.KOREAN to "빠른 장전"),
                )
            val hyperChargeRequest =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000300L,
                    names = mapOf(Language.ENGLISH to "Supercharged"),
                )

            // when
            sut.createGadget(brawlerBrawlStarsId = 16000000L, request = gadgetRequest)
            sut.createHyperCharge(brawlerBrawlStarsId = 16000000L, request = hyperChargeRequest)

            // then
            assertThat(gadgetJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            assertThat(hyperChargeJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            val gadgetNames =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.GADGET_NAME, listOf(23000200L))
                    .associate { it.lang to it.content }
            assertThat(gadgetNames).containsExactlyInAnyOrderEntriesOf(mapOf(Language.KOREAN to "빠른 장전"))
            val hyperChargeNames =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.HYPERCHARGE_NAME, listOf(23000300L))
                    .associate { it.lang to it.content }
            assertThat(hyperChargeNames).containsExactlyInAnyOrderEntriesOf(mapOf(Language.ENGLISH to "Supercharged"))
        }

        @Test
        fun `이름 없이 스타파워를 등록하면 메시지가 저장되지 않는다`() {
            // given
            val request = BrawlerSubItemCreateRequest(brawlStarsId = 23000400L)

            // when
            sut.createStarPower(brawlerBrawlStarsId = 16000000L, request = request)

            // then
            assertThat(starPowerJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            assertThat(
                messageJpaRepository.findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, listOf(23000400L)),
            ).isEmpty()
        }
    }
```

- [ ] **Step 2: 테스트가 컴파일 실패(names 파라미터 없음)하는 것 확인**

Run (in `imstargg-backend/`): `./gradlew :core:core-admin:compileTestKotlin`
Expected: FAIL — `BrawlerSubItemCreateRequest` 에 `names` 파라미터가 없어 컴파일 에러 (`no value passed for parameter` 또는 `cannot find a parameter with this name: names`)

- [ ] **Step 3: 최소 구현**

`BrawlerSubItemCreateRequest.kt` 전체를 다음으로 교체:

```kotlin
package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class BrawlerSubItemCreateRequest(
    val brawlStarsId: Long,
    val names: Map<Language, String> = emptyMap(),
)
```

`BrawlerService.kt` 의 세 메서드에 `doUpdateMessages` 호출을 한 줄씩 추가 (187-209 라인 영역):

```kotlin
    @Transactional
    fun createStarPower(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        starPowerJpaRepository.save(StarPowerEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.STARPOWER_NAME, request.brawlStarsId, request.names)
    }

    @Transactional
    fun createGadget(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        gadgetJpaRepository.save(GadgetEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.GADGET_NAME, request.brawlStarsId, request.names)
    }

    @Transactional
    fun createHyperCharge(
        brawlerBrawlStarsId: Long,
        request: BrawlerSubItemCreateRequest,
    ) {
        hyperChargeJpaRepository.save(HyperChargeEntity(request.brawlStarsId, brawlerBrawlStarsId))
        doUpdateMessages(MessageType.HYPERCHARGE_NAME, request.brawlStarsId, request.names)
    }
```

- [ ] **Step 4: 테스트 통과 확인**

Run (in `imstargg-backend/`, 필요 시 먼저 `docker-compose up -d`):
`./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BrawlerServiceTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: 린트 확인**

Run (in `imstargg-backend/`): `./gradlew :core:core-admin:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BrawlerSubItemCreateRequest.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BrawlerService.kt \
        imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BrawlerServiceTest.kt
git commit -m "feat(core-admin): 하위 아이템 생성 시 이름 함께 저장"
```

---

### Task 2: admin-frontend — 하위 아이템 추가 다이얼로그에 이름·이미지 통합

**Files:**
- Modify: `imstargg-admin-frontend/lib/api.ts:84-88,113-117,142-146` (createStarPower/createGadget/createHyperCharge)
- Modify: `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx` (SubItemSection + 3개 사용처)

**Interfaces:**
- Consumes: Task 1 의 API 계약 — `POST /brawlers/{id}/star-powers|gadgets|hyper-charges` body `{ brawlStarsId, names }`; 기존 `uploadStarPowerImage/uploadGadgetImage/uploadHyperChargeImage(brawlerBrawlStarsId, itemBrawlStarsId, file)`; 기존 컴포넌트 `PendingImageInput({ file, onChange })`
- Produces: `createStarPower/createGadget/createHyperCharge(brawlerBrawlStarsId: number, brawlStarsId: number, names: Record<string, string>): Promise<void>` — Task 3 이 동일 시그니처를 사용.

- [ ] **Step 1: api.ts 시그니처 변경**

`lib/api.ts` 의 세 함수를 다음으로 교체 (다른 함수는 변경 없음):

```typescript
export const createStarPower = (
  brawlerBrawlStarsId: number,
  brawlStarsId: number,
  names: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/star-powers`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId, names }),
  });
```

```typescript
export const createGadget = (
  brawlerBrawlStarsId: number,
  brawlStarsId: number,
  names: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/gadgets`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId, names }),
  });
```

```typescript
export const createHyperCharge = (
  brawlerBrawlStarsId: number,
  brawlStarsId: number,
  names: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/hyper-charges`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId, names }),
  });
```

- [ ] **Step 2: SubItemSection 다이얼로그 확장**

`app/brawlers/[brawlStarsId]/page.tsx` 에서:

(a) import 에 `PendingImageInput` 추가:

```tsx
import { PendingImageInput } from "@/components/pending-image-input";
```

(b) `SubItemSection` 컴포넌트(현재 99-171 라인)를 다음으로 교체. `onAdd` prop 시그니처가 `(brawlStarsId: number, names: Record<string, string>) => Promise<void>` 로 바뀌고, 다이얼로그에 이름·이미지 입력이 추가되며, 이미지 업로드는 기존 `onUploadImage` prop 을 재사용한다:

```tsx
function SubItemSection({
  title,
  items,
  onAdd,
  onSaveMessages,
  onUploadImage,
  onReload,
}: {
  title: string;
  items: (StarPower | Gadget | HyperCharge)[];
  onAdd: (brawlStarsId: number, names: Record<string, string>) => Promise<void>;
  onSaveMessages: (itemBrawlStarsId: number, messages: Record<string, string>) => Promise<void>;
  onUploadImage: (itemBrawlStarsId: number, file: File) => Promise<void>;
  onReload: () => void;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [addId, setAddId] = useState("");
  const [addKo, setAddKo] = useState("");
  const [addEn, setAddEn] = useState("");
  const [addFile, setAddFile] = useState<File | null>(null);
  const [adding, setAdding] = useState(false);

  const resetAddForm = () => {
    setAddId("");
    setAddKo("");
    setAddEn("");
    setAddFile(null);
  };

  const handleAdd = async () => {
    const id = Number(addId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    setAdding(true);
    try {
      await onAdd(id, { KOREAN: addKo, ENGLISH: addEn });
    } catch {
      toast.error("등록에 실패했습니다.");
      setAdding(false);
      return;
    }
    if (addFile) {
      try {
        await onUploadImage(id, addFile);
      } catch {
        toast.error("등록됐지만 이미지 업로드에 실패했습니다. 아래 목록에서 다시 업로드하세요.");
      }
    }
    toast.success("등록되었습니다.");
    setAdding(false);
    setAddOpen(false);
    resetAddForm();
    onReload();
  };

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>
          {title} <span className="text-sm font-normal text-muted-foreground">· {items.length}</span>
        </CardTitle>
        <Dialog open={addOpen} onOpenChange={setAddOpen}>
          <DialogTrigger render={<Button size="sm" />}>{title} 추가</DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>{title} 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input type="number" value={addId} onChange={(e) => setAddId(e.target.value)} />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <Label>이름 (KO)</Label>
                  <Input value={addKo} onChange={(e) => setAddKo(e.target.value)} />
                </div>
                <div>
                  <Label>이름 (EN)</Label>
                  <Input value={addEn} onChange={(e) => setAddEn(e.target.value)} />
                </div>
              </div>
              <div>
                <Label>이미지 (선택)</Label>
                <PendingImageInput file={addFile} onChange={setAddFile} />
              </div>
              <Button onClick={handleAdd} disabled={adding} className="w-full">
                {adding ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.length === 0 && (
          <p className="text-sm text-muted-foreground">등록된 항목이 없습니다.</p>
        )}
        {items.map((item) => (
          <SubItemRow
            key={item.brawlStarsId}
            item={item}
            onSaveMessages={(msgs) => onSaveMessages(item.brawlStarsId, msgs)}
            onUploadImage={(file) => onUploadImage(item.brawlStarsId, file)}
            onReload={onReload}
          />
        ))}
      </CardContent>
    </Card>
  );
}
```

(c) 페이지 본문의 3개 `SubItemSection` 사용처(현재 311-345 라인)의 `onAdd` 를 다음으로 교체 (`loadDetail()` 호출은 `handleAdd` 의 `onReload()` 로 일원화되므로 제거):

```tsx
        <SubItemSection
          title="가젯"
          items={detail.gadgets}
          onAdd={async (id, names) => {
            await createGadget(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateGadgetMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadGadgetImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="스타파워"
          items={detail.starPowers}
          onAdd={async (id, names) => {
            await createStarPower(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateStarPowerMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadStarPowerImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="하이퍼차지"
          items={detail.hyperCharges}
          onAdd={async (id, names) => {
            await createHyperCharge(brawlStarsId, id, names);
          }}
          onSaveMessages={(itemId, msgs) => updateHyperChargeMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadHyperChargeImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />
```

- [ ] **Step 3: 검증**

Run (in `imstargg-admin-frontend/`): `pnpm lint && pnpm exec tsc --noEmit && pnpm build`
Expected: 모두 성공. 만약 tsc 가 `createGadget` 등의 인자 수 오류를 다른 파일에서 보고하면 그 호출부는 Task 3 범위이므로 이 태스크에서 함께 고치지 말 것 — 이 시점에는 상세 페이지 외 호출부가 없어야 정상.

- [ ] **Step 4: Commit**

```bash
git add imstargg-admin-frontend/lib/api.ts "imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx"
git commit -m "feat(admin-frontend): 하위 아이템 추가 다이얼로그에 이름·이미지 통합"
```

---

### Task 3: admin-frontend — 브롤러 등록 폼에 하위 아이템 등록 통합

**Files:**
- Modify: `imstargg-admin-frontend/app/brawlers/new/page.tsx`

**Interfaces:**
- Consumes: Task 2 의 `createStarPower/createGadget/createHyperCharge(brawlerBrawlStarsId, brawlStarsId, names)`, 기존 `uploadStarPowerImage/uploadGadgetImage/uploadHyperChargeImage(brawlerBrawlStarsId, itemBrawlStarsId, file)`, `PendingImageInput({ file, onChange })`
- Produces: 없음 (말단 UI)

- [ ] **Step 1: pending 하위 아이템 타입·컴포넌트·제출 로직 추가**

`app/brawlers/new/page.tsx` 를 다음 전체 내용으로 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  getGears,
  createBrawler,
  uploadBrawlerImage,
  createGadget,
  uploadGadgetImage,
  createStarPower,
  uploadStarPowerImage,
  createHyperCharge,
  uploadHyperChargeImage,
} from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import type { Gear } from "@/lib/types";
import { toast } from "sonner";

const RARITIES = [
  "STARTING_BRAWLER",
  "RARE",
  "SUPER_RARE",
  "EPIC",
  "MYTHIC",
  "LEGENDARY",
  "ULTRA_LEGENDARY",
];

const ROLES = [
  "TANK",
  "ASSASSIN",
  "SUPPORT",
  "CONTROLLER",
  "DAMAGE_DEALER",
  "MARKSMAN",
  "ARTILLERY",
];

type PendingSubItem = {
  key: number;
  brawlStarsId: string;
  ko: string;
  en: string;
  file: File | null;
};

let pendingSubItemKey = 0;
const nextPendingSubItemKey = () => ++pendingSubItemKey;

function PendingSubItemsCard({
  title,
  items,
  onChange,
}: {
  title: string;
  items: PendingSubItem[];
  onChange: (items: PendingSubItem[]) => void;
}) {
  const updateItem = (key: number, patch: Partial<PendingSubItem>) =>
    onChange(items.map((it) => (it.key === key ? { ...it, ...patch } : it)));

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between">
        <CardTitle>{title} (선택)</CardTitle>
        <Button
          size="sm"
          variant="outline"
          onClick={() =>
            onChange([
              ...items,
              { key: nextPendingSubItemKey(), brawlStarsId: "", ko: "", en: "", file: null },
            ])
          }
        >
          행 추가
        </Button>
      </CardHeader>
      <CardContent className="space-y-3">
        {items.length === 0 && (
          <p className="text-sm text-muted-foreground">함께 등록할 항목이 없으면 비워두세요.</p>
        )}
        {items.map((item) => (
          <div key={item.key} className="space-y-3 rounded-md border p-3">
            <div className="flex items-end gap-2">
              <div className="flex-1">
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={item.brawlStarsId}
                  onChange={(e) => updateItem(item.key, { brawlStarsId: e.target.value })}
                />
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onChange(items.filter((it) => it.key !== item.key))}
              >
                삭제
              </Button>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={item.ko} onChange={(e) => updateItem(item.key, { ko: e.target.value })} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={item.en} onChange={(e) => updateItem(item.key, { en: e.target.value })} />
              </div>
            </div>
            <div>
              <Label>이미지 (선택)</Label>
              <PendingImageInput file={item.file} onChange={(f) => updateItem(item.key, { file: f })} />
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

export default function BrawlerCreatePage() {
  const router = useRouter();
  const [gears, setGears] = useState<Gear[]>([]);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "", ko: "", en: "" });
  const [selectedGearIds, setSelectedGearIds] = useState<number[]>([]);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [gadgets, setGadgets] = useState<PendingSubItem[]>([]);
  const [starPowers, setStarPowers] = useState<PendingSubItem[]>([]);
  const [hyperCharges, setHyperCharges] = useState<PendingSubItem[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getGears().then(setGears);
  }, []);

  const toggleGear = (id: number) => {
    setSelectedGearIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id],
    );
  };

  const subItemGroups = [
    { label: "가젯", items: gadgets, create: createGadget, upload: uploadGadgetImage },
    { label: "스타파워", items: starPowers, create: createStarPower, upload: uploadStarPowerImage },
    { label: "하이퍼차지", items: hyperCharges, create: createHyperCharge, upload: uploadHyperChargeImage },
  ];

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("ID, rarity, role을 입력해주세요.");
      return;
    }
    if (subItemGroups.some((group) => group.items.some((item) => !Number(item.brawlStarsId)))) {
      toast.error("하위 아이템의 BrawlStars ID를 입력해주세요. 필요 없는 행은 삭제하세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createBrawler({
        brawlStarsId: id,
        rarity: form.rarity,
        role: form.role,
        names: { KOREAN: form.ko, ENGLISH: form.en },
        gearBrawlStarsIds: selectedGearIds,
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    const failed: string[] = [];
    if (imageFile) {
      try {
        await uploadBrawlerImage(id, imageFile);
      } catch {
        failed.push("브롤러 이미지");
      }
    }
    for (const group of subItemGroups) {
      for (const item of group.items) {
        const itemId = Number(item.brawlStarsId);
        try {
          await group.create(id, itemId, { KOREAN: item.ko, ENGLISH: item.en });
        } catch {
          failed.push(`${group.label} ${itemId} 등록`);
          continue;
        }
        if (item.file) {
          try {
            await group.upload(id, itemId, item.file);
          } catch {
            failed.push(`${group.label} ${itemId} 이미지`);
          }
        }
      }
    }
    if (failed.length > 0) {
      toast.error(`일부 항목에 실패했습니다: ${failed.join(", ")} — 상세 페이지에서 다시 시도하세요.`);
    }
    toast.success("브롤러가 등록되었습니다.");
    router.push(`/brawlers/${id}`);
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">브롤러 등록</h2>
      <div className="max-w-2xl space-y-6">
        <Card>
          <CardHeader><CardTitle>기본 정보</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label>BrawlStars ID</Label>
              <Input
                type="number"
                value={form.brawlStarsId}
                onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {RARITIES.map((r) => (<SelectItem key={r} value={r}>{r}</SelectItem>))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Role</Label>
                <Select value={form.role} onValueChange={(v) => setForm({ ...form, role: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (<SelectItem key={r} value={r}>{r}</SelectItem>))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>기어 (선택 · 클릭하여 토글)</CardTitle></CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-3">
              {gears.map((g) => {
                const selected = selectedGearIds.includes(g.brawlStarsId);
                return (
                  <button
                    key={g.brawlStarsId}
                    type="button"
                    onClick={() => toggleGear(g.brawlStarsId)}
                    className={`flex w-20 flex-col items-center gap-1 rounded-md border p-2 ${
                      selected ? "border-primary ring-2 ring-primary" : "border-border"
                    }`}
                  >
                    <ImageThumbnail url={g.imageUrl} alt={g.names.KOREAN || String(g.brawlStarsId)} />
                    <span className="w-full truncate text-center text-xs">
                      {g.names.KOREAN || g.brawlStarsId}
                    </span>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        <PendingSubItemsCard title="가젯" items={gadgets} onChange={setGadgets} />
        <PendingSubItemsCard title="스타파워" items={starPowers} onChange={setStarPowers} />
        <PendingSubItemsCard title="하이퍼차지" items={hyperCharges} onChange={setHyperCharges} />

        <Card>
          <CardHeader><CardTitle>이미지 (선택)</CardTitle></CardHeader>
          <CardContent>
            <PendingImageInput file={imageFile} onChange={setImageFile} />
          </CardContent>
        </Card>

        <div className="flex gap-2">
          <Button onClick={handleCreate} disabled={submitting}>
            {submitting ? "등록 중..." : "등록"}
          </Button>
          <Button variant="outline" onClick={() => router.push("/brawlers")}>취소</Button>
        </div>
      </div>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 검증**

Run (in `imstargg-admin-frontend/`): `pnpm lint && pnpm exec tsc --noEmit && pnpm build`
Expected: 모두 성공

- [ ] **Step 3: Commit**

```bash
git add imstargg-admin-frontend/app/brawlers/new/page.tsx
git commit -m "feat(admin-frontend): 브롤러 등록 폼에 하위 아이템 등록 통합"
```

---

### Task 4: admin-frontend — 버피 등록에 이미지 통합

**Files:**
- Modify: `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx` (BrawlerDetailPage 의 버피 카드 + handleCreateBuffy)

**Interfaces:**
- Consumes: 기존 `createBrawlerBuffy(brawlStarsId, type)`, `uploadBrawlerBuffyImage(brawlStarsId, type, file)`, `PendingImageInput({ file, onChange })`, `BuffyType`
- Produces: 없음 (말단 UI). 백엔드 변경 없음.

- [ ] **Step 1: 버피 등록 핸들러·카드에 이미지 선택 추가**

`app/brawlers/[brawlStarsId]/page.tsx` 에서:

(a) 파일 상단에 `PendingImageInput` import 가 없다면 추가 (Task 2 에서 이미 추가됐다면 생략):

```tsx
import { PendingImageInput } from "@/components/pending-image-input";
```

그리고 `BrawlerDetailPage` 의 state 선언부(`selectedGearIds` 아래)에 추가:

```tsx
  const [buffyFiles, setBuffyFiles] = useState<Record<BuffyType, File | null>>({
    GADGET: null,
    STAR_POWER: null,
    HYPER_CHARGE: null,
  });
```

(b) `handleCreateBuffy`(현재 208-216 라인)를 다음으로 교체:

```tsx
  const handleCreateBuffy = async (type: BuffyType) => {
    try {
      await createBrawlerBuffy(brawlStarsId, type);
    } catch {
      toast.error("등록에 실패했습니다.");
      return;
    }
    const file = buffyFiles[type];
    if (file) {
      try {
        await uploadBrawlerBuffyImage(brawlStarsId, type, file);
      } catch {
        toast.error("등록됐지만 이미지 업로드에 실패했습니다. 이미지를 다시 업로드하세요.");
      }
    }
    toast.success("버피가 등록되었습니다.");
    setBuffyFiles((prev) => ({ ...prev, [type]: null }));
    loadDetail();
  };
```

(c) 버피 카드의 미등록 분기(현재 `<p className="text-sm text-muted-foreground">미등록</p>`, 367 라인 부근)를 다음으로 교체:

```tsx
                      <PendingImageInput
                        file={buffyFiles[type]}
                        onChange={(f) => setBuffyFiles((prev) => ({ ...prev, [type]: f }))}
                      />
```

등록/삭제 버튼 영역(370-380 라인)은 변경하지 않는다 — "등록" 버튼이 이제 이미지까지 함께 처리한다.

- [ ] **Step 2: 검증**

Run (in `imstargg-admin-frontend/`): `pnpm lint && pnpm exec tsc --noEmit && pnpm build`
Expected: 모두 성공

- [ ] **Step 3: Commit**

```bash
git add "imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx"
git commit -m "feat(admin-frontend): 버피 등록에 이미지 통합"
```

---

### Task 5: 종합 검증

**Files:** 없음 (검증만)

**Interfaces:**
- Consumes: Task 1-4 전체
- Produces: 없음

- [ ] **Step 1: 백엔드 전체 core-admin 테스트**

Run (in `imstargg-backend/`): `./gradlew :core:core-admin:test :core:core-admin:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 프론트 최종 검증**

Run (in `imstargg-admin-frontend/`): `pnpm lint && pnpm exec tsc --noEmit && pnpm build`
Expected: 모두 성공

- [ ] **Step 3: (선택) 수동 확인**

로컬에서 core-admin(`./gradlew :core:core-admin:bootRun`)과 admin-frontend(`pnpm dev`)를 띄우고:
- 브롤러 상세에서 "가젯 추가" → ID+이름+이미지 한 번에 등록되는지
- `/brawlers/new` 에서 하위 아이템 행 추가 후 등록 → 상세로 이동해 모두 반영됐는지
- 미등록 버피에 이미지 선택 후 등록 → 한 번에 반영되는지
