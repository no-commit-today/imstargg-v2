# 계획 3 — 프론트: 브롤러 상세 단일 페이지 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 브롤러 상세 페이지의 탭을 제거하고, 모든 섹션을 한 페이지에 **이름 → 기어 → 가젯 → 스타파워 → 하이퍼차지 → 버피** 순서로 배치한다. 기어 선택을 이미지 타일 그리드로 바꾸고, 하위 항목(스타파워/가젯/하이퍼차지)을 다이얼로그 대신 인라인 카드로 바로 편집한다. 버피는 기본정보에서 분리해 맨 아래 독립 섹션으로 둔다.

**Architecture:** 프론트 전용 변경(백엔드 무변경). 상세 조회 API(`getBrawlerDetail`)가 이름·rarity·role·하위항목·버피·`linkedGearBrawlStarsIds` 를 모두 내려준다. 단, 이 응답의 `allGears` 는 `imageUrl` 이 `null` 이므로, 기어 이미지 그리드는 **별도로 `getGears()`**(이미지 포함)를 호출해 그린다. 저장은 기존 섹션별 엔드포인트(이름/이미지/기어연결/각 하위항목)를 그대로 재사용한다(granular 저장 유지). 단일 파일 `app/brawlers/[brawlStarsId]/page.tsx` 만 교체한다.

**Tech Stack:** Next.js 16 (app router) + React 19 + TS, base-ui/shadcn, Tailwind, sonner. pnpm. (의존성 설치됨.)

**작업 디렉터리:** 명령은 `imstargg-admin-frontend/` 에서 실행. 파일 경로는 저장소 루트 기준.

**테스트 전략:** 단위 테스트 프레임워크 없음 → `npx tsc --noEmit` + `pnpm lint`(둘 다 exit 0) + `pnpm build` + 수동 스모크. 기준선 green.

**Lint 주의:** 이 프로젝트 eslint 는 `react-hooks/set-state-in-effect` 를 에러로 막는다. 아래 코드는 `useEffect` 안에서 `getX().then(setState)`(비동기 콜백)만 호출하므로 위반하지 않는다 — 구조를 바꾸지 말 것.

**공통 규칙:** 커밋 메시지 끝에 트레일러 — `-m "<title>" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`.

---

## File Structure

**수정 (전체 교체):**
- `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx` — 탭 제거, 단일 페이지 재구성, 기어 이미지 그리드, 하위 항목 인라인 편집, 버피 독립 섹션.

**변경 없음:** `lib/api.ts`(기존 함수 그대로 사용), `lib/types.ts`, 백엔드, 다른 페이지/컴포넌트.

---

## Task 1: 브롤러 상세 페이지 단일화

**Files:**
- Modify: `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx` (전체 교체)

- [ ] **Step 1: 파일 전체 교체** — `app/brawlers/[brawlStarsId]/page.tsx` 전체를 아래로 교체:

```tsx
"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams } from "next/navigation";
import { AdminLayout } from "../../admin-layout";
import { Button } from "@/components/ui/button";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  getBrawlerDetail,
  getGears,
  updateBrawlerMessages,
  uploadBrawlerImage,
  createBrawlerBuffy,
  uploadBrawlerBuffyImage,
  deleteBrawlerBuffy,
  updateBrawlerGears,
  createStarPower,
  updateStarPowerMessages,
  uploadStarPowerImage,
  createGadget,
  updateGadgetMessages,
  uploadGadgetImage,
  createHyperCharge,
  updateHyperChargeMessages,
  uploadHyperChargeImage,
} from "@/lib/api";
import type { BrawlerDetail, BuffyType, Gear, StarPower, Gadget, HyperCharge } from "@/lib/types";
import { toast } from "sonner";
import { ImageUploadField } from "@/components/image-upload-field";

const BUFFY_TYPES: BuffyType[] = ["GADGET", "STAR_POWER", "HYPER_CHARGE"];
const BUFFY_LABELS: Record<BuffyType, string> = {
  GADGET: "가젯 버피",
  STAR_POWER: "스타파워 버피",
  HYPER_CHARGE: "하이퍼차지 버피",
};

// 하위 항목(스타파워/가젯/하이퍼차지) 인라인 편집 행
function SubItemRow({
  item,
  onSaveMessages,
  onUploadImage,
  onReload,
}: {
  item: StarPower | Gadget | HyperCharge;
  onSaveMessages: (messages: Record<string, string>) => Promise<void>;
  onUploadImage: (file: File) => Promise<void>;
  onReload: () => void;
}) {
  const [ko, setKo] = useState(item.names.KOREAN || "");
  const [en, setEn] = useState(item.names.ENGLISH || "");

  const handleSave = async () => {
    try {
      await onSaveMessages({ KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      onReload();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  return (
    <div className="space-y-3 rounded-md border p-3">
      <p className="text-xs text-muted-foreground">ID {item.brawlStarsId}</p>
      <div className="grid grid-cols-2 gap-2">
        <div>
          <Label>이름 (KO)</Label>
          <Input value={ko} onChange={(e) => setKo(e.target.value)} />
        </div>
        <div>
          <Label>이름 (EN)</Label>
          <Input value={en} onChange={(e) => setEn(e.target.value)} />
        </div>
      </div>
      <Button size="sm" onClick={handleSave}>이름 저장</Button>
      <div>
        <Label>이미지</Label>
        <ImageUploadField
          currentImageUrl={item.imageUrl}
          onUpload={onUploadImage}
          onUploaded={onReload}
          previewClassName="max-h-24"
        />
      </div>
    </div>
  );
}

// 하위 항목 섹션 (추가 다이얼로그 + 인라인 행 목록)
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
  onAdd: (brawlStarsId: number) => Promise<void>;
  onSaveMessages: (itemBrawlStarsId: number, messages: Record<string, string>) => Promise<void>;
  onUploadImage: (itemBrawlStarsId: number, file: File) => Promise<void>;
  onReload: () => void;
}) {
  const [addOpen, setAddOpen] = useState(false);
  const [addId, setAddId] = useState("");

  const handleAdd = async () => {
    const id = Number(addId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    try {
      await onAdd(id);
      toast.success("등록되었습니다.");
      setAddOpen(false);
      setAddId("");
    } catch {
      toast.error("등록에 실패했습니다.");
    }
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
              <Button onClick={handleAdd} className="w-full">등록</Button>
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

export default function BrawlerDetailPage() {
  const params = useParams();
  const brawlStarsId = Number(params.brawlStarsId);
  const [detail, setDetail] = useState<BrawlerDetail | null>(null);
  const [gears, setGears] = useState<Gear[]>([]);
  const [ko, setKo] = useState("");
  const [en, setEn] = useState("");
  const [selectedGearIds, setSelectedGearIds] = useState<Set<number>>(new Set());

  const loadDetail = useCallback(() => {
    getBrawlerDetail(brawlStarsId).then((d) => {
      setDetail(d);
      setKo(d.names.KOREAN || "");
      setEn(d.names.ENGLISH || "");
      setSelectedGearIds(new Set(d.linkedGearBrawlStarsIds));
    });
  }, [brawlStarsId]);

  useEffect(() => {
    loadDetail();
    getGears().then(setGears);
  }, [loadDetail]);

  if (!detail) return <AdminLayout><p>로딩 중...</p></AdminLayout>;

  const handleSaveMessages = async () => {
    try {
      await updateBrawlerMessages(brawlStarsId, { KOREAN: ko, ENGLISH: en });
      toast.success("이름이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const handleCreateBuffy = async (type: BuffyType) => {
    try {
      await createBrawlerBuffy(brawlStarsId, type);
      toast.success("버피가 등록되었습니다.");
      loadDetail();
    } catch {
      toast.error("등록에 실패했습니다.");
    }
  };

  const handleDeleteBuffy = async (type: BuffyType) => {
    if (!confirm(`${BUFFY_LABELS[type]}를 삭제할까요? 이미지도 함께 삭제됩니다.`)) return;
    try {
      await deleteBrawlerBuffy(brawlStarsId, type);
      toast.success("버피가 삭제되었습니다.");
      loadDetail();
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  const handleSaveGears = async () => {
    try {
      await updateBrawlerGears(brawlStarsId, Array.from(selectedGearIds));
      toast.success("기어 연결이 저장되었습니다.");
      loadDetail();
    } catch {
      toast.error("저장에 실패했습니다.");
    }
  };

  const toggleGear = (gearId: number) => {
    setSelectedGearIds((prev) => {
      const next = new Set(prev);
      if (next.has(gearId)) next.delete(gearId);
      else next.add(gearId);
      return next;
    });
  };

  return (
    <AdminLayout>
      <h2 className="mb-4 text-2xl font-bold">브롤러 상세 - {brawlStarsId}</h2>

      <div className="max-w-3xl space-y-6">
        <Card>
          <CardHeader><CardTitle>이름 · 기본정보</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              ID {detail.brawlStarsId} · {detail.rarity} · {detail.role}
            </p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>이름 (KO)</Label>
                <Input value={ko} onChange={(e) => setKo(e.target.value)} />
              </div>
              <div>
                <Label>이름 (EN)</Label>
                <Input value={en} onChange={(e) => setEn(e.target.value)} />
              </div>
            </div>
            <Button onClick={handleSaveMessages}>이름 저장</Button>
            <div>
              <Label>메인 이미지</Label>
              <ImageUploadField
                currentImageUrl={detail.imageUrl}
                onUpload={(file) => uploadBrawlerImage(brawlStarsId, file)}
                onUploaded={loadDetail}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between">
            <CardTitle>기어</CardTitle>
            <Button size="sm" onClick={handleSaveGears}>기어 연결 저장</Button>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-3">
              {gears.map((gear) => {
                const selected = selectedGearIds.has(gear.brawlStarsId);
                return (
                  <button
                    key={gear.brawlStarsId}
                    type="button"
                    onClick={() => toggleGear(gear.brawlStarsId)}
                    className={`flex w-20 flex-col items-center gap-1 rounded-md border p-2 ${
                      selected ? "border-primary ring-2 ring-primary" : "border-border"
                    }`}
                  >
                    <ImageThumbnail url={gear.imageUrl} alt={gear.names.KOREAN || String(gear.brawlStarsId)} />
                    <span className="w-full truncate text-center text-xs">
                      {gear.names.KOREAN || gear.brawlStarsId}
                    </span>
                    <span className="text-[10px] text-muted-foreground">{gear.rarity}</span>
                  </button>
                );
              })}
            </div>
          </CardContent>
        </Card>

        <SubItemSection
          title="가젯"
          items={detail.gadgets}
          onAdd={async (id) => {
            await createGadget(brawlStarsId, id);
            loadDetail();
          }}
          onSaveMessages={(itemId, msgs) => updateGadgetMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadGadgetImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="스타파워"
          items={detail.starPowers}
          onAdd={async (id) => {
            await createStarPower(brawlStarsId, id);
            loadDetail();
          }}
          onSaveMessages={(itemId, msgs) => updateStarPowerMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadStarPowerImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <SubItemSection
          title="하이퍼차지"
          items={detail.hyperCharges}
          onAdd={async (id) => {
            await createHyperCharge(brawlStarsId, id);
            loadDetail();
          }}
          onSaveMessages={(itemId, msgs) => updateHyperChargeMessages(brawlStarsId, itemId, msgs)}
          onUploadImage={(itemId, file) => uploadHyperChargeImage(brawlStarsId, itemId, file)}
          onReload={loadDetail}
        />

        <Card>
          <CardHeader><CardTitle>버피</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            {BUFFY_TYPES.map((type) => {
              const buffy = detail.buffies.find((b) => b.type === type);
              return (
                <div
                  key={type}
                  className="flex items-start gap-4 border-b pb-4 last:border-b-0 last:pb-0"
                >
                  <div className="flex-1 space-y-2">
                    <p className="text-sm font-medium">{BUFFY_LABELS[type]}</p>
                    {buffy ? (
                      <ImageUploadField
                        currentImageUrl={buffy.imageUrl}
                        onUpload={(file) => uploadBrawlerBuffyImage(brawlStarsId, type, file)}
                        onUploaded={loadDetail}
                        previewClassName="max-h-24"
                      />
                    ) : (
                      <p className="text-sm text-muted-foreground">미등록</p>
                    )}
                  </div>
                  <div className="pt-5">
                    {buffy ? (
                      <Button variant="outline" size="sm" onClick={() => handleDeleteBuffy(type)}>
                        삭제
                      </Button>
                    ) : (
                      <Button size="sm" onClick={() => handleCreateBuffy(type)}>
                        등록
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </CardContent>
        </Card>
      </div>
    </AdminLayout>
  );
}
```

- [ ] **Step 2: 검증** — Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint`
Expected: 둘 다 에러 없이 종료(출력 없음 = 성공). 특히 `Table`/`Tabs` 등 더 이상 쓰지 않는 import 가 남아 unused 로 걸리지 않는지 확인(위 코드엔 없음).

- [ ] **Step 3: 커밋**

```bash
cd imstargg-admin-frontend
git add app/brawlers/[brawlStarsId]/page.tsx
git commit -m "feat(admin-frontend): 브롤러 상세 단일 페이지화(탭 제거·기어 이미지 그리드·하위항목 인라인 편집·버피 분리)" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 최종 검증 (빌드 + 수동 스모크)

**Files:** 없음(검증만).

- [ ] **Step 1: 타입체크 + 린트 + 빌드**

Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint && pnpm build`
Expected: 모두 성공. 라우트 목록에 `/brawlers/[brawlStarsId]` 가 그대로 존재.

- [ ] **Step 2: 수동 스모크 (백엔드 연동) — 선택적, 사람이 수행 권장**

```bash
# 터미널 A
cd imstargg-backend && ./gradlew :core:core-admin:bootRun
# 터미널 B
cd imstargg-admin-frontend && pnpm dev
```
브라우저에서 임의 브롤러 상세(`/brawlers/{id}`) 진입 후 확인:
- 탭이 없고, 섹션이 **이름 → 기어 → 가젯 → 스타파워 → 하이퍼차지 → 버피** 순서로 한 페이지에 보인다.
- 기어 섹션이 **이미지 타일 그리드**로 보이고(이미지 등록된 기어는 썸네일 표시), 클릭으로 선택 토글 → "기어 연결 저장" 동작.
- 가젯/스타파워/하이퍼차지가 **인라인 카드**로 보이고, 이름(KO/EN) 저장·이미지 업로드가 즉시 동작.
- 버피가 맨 아래 독립 섹션에서 등록/삭제·이미지 동작.

---

## 가정 / 참고

- 기어 이미지는 `getGears()`(이미지 포함)에서 가져오고, 선택 상태는 `getBrawlerDetail` 의 `linkedGearBrawlStarsIds` 로 초기화한다. (`getBrawlerDetail` 의 `allGears.imageUrl` 은 백엔드에서 `null` 이라 사용하지 않는다.)
- 하위 항목 "추가" 는 기존과 동일하게 하위 항목 고유 `brawlStarsId` 를 입력받는 소형 다이얼로그를 유지한다(인라인 편집은 기존 항목에 대해서만).
- 백엔드/다른 페이지 변경 없음. 저장은 모두 기존 granular 엔드포인트 재사용.
- (후속 정리 후보, 본 계획 범위 밖) `getBrawlerDetail` 응답의 `allGears` 가 이제 프론트에서 미사용이므로, 추후 백엔드에서 제거하거나 이미지까지 채우도록 정리할 수 있다.
