# 계획 2 — 프론트: 통합 등록 폼 + 순차 이미지 업로드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `imstargg-admin-frontend` 의 브롤러/기어/이벤트/이벤트 모드 등록 폼을, 이미지를 포함한 모든 데이터를 한 폼에서 입력하도록 개편하고, 제출 시 `생성 API → 이미지 업로드 API` 를 순차 호출한다.

**Architecture:** 계획 1에서 확장한 생성 API(`names`/`gearBrawlStarsIds`/`mapNames`/`playerCounts`/`color`)를 호출하도록 `lib/api.ts` 클라이언트 함수 시그니처를 넓힌다. 각 등록 폼은 모든 필드 + (해당 시) 이미지 파일 선택을 모으고, 제출 시 `createX(data)` 성공 후 파일이 있으면 `uploadXImage(id, file)` 를 이어서 호출한다(부분 실패 허용). 이미지는 "선택 후 보관 → 생성 뒤 업로드" 패턴이라 즉시 업로드하는 기존 `ImageUploadField` 대신 새 `PendingImageInput`(파일을 부모 state 로 들고 미리보기만) 을 쓴다. 브롤러는 기어 이미지 그리드 때문에 전용 페이지(`/brawlers/new`), 나머지는 기존 목록 페이지의 다이얼로그를 확장한다.

**Tech Stack:** Next.js 16 (app router) + React 19 + TypeScript, base-ui/react 기반 shadcn 컴포넌트, Tailwind, sonner(toast). 패키지 매니저 pnpm.

**작업 디렉터리:** 모든 명령은 `imstargg-admin-frontend/` 에서 실행. 파일 경로는 저장소 루트 기준. 의존성은 이미 `pnpm install` 됨.

**테스트 전략(중요):** 이 프로젝트는 단위 테스트 프레임워크가 없다(`package.json` scripts = dev/build/start/lint). 따라서 각 태스크는 **정적 게이트**로 검증한다: `npx tsc --noEmit` (타입) + `pnpm lint` (eslint). 마지막 태스크에서 `pnpm build` 와 **수동 스모크**(백엔드 띄우고 폼 제출)로 마무리. 기준선은 현재 green 임을 확인했다.

**비표준 Next.js 주의:** `imstargg-admin-frontend/AGENTS.md` 는 "이 Next.js 는 학습 데이터와 다를 수 있으니 코드 작성 전 `node_modules/next/dist/docs/` 의 관련 가이드를 읽으라"고 경고한다. 본 계획의 코드는 저장소의 기존 페이지 패턴(`"use client"`, `useState`, `useRouter`/`useParams` from `next/navigation`, `next/link`)을 그대로 따르므로 새 API 를 도입하지 않는다. 의심스러우면 기존 페이지(`app/gears/[brawlStarsId]/edit/page.tsx` 등)를 참고하라.

**공통 규칙:** 모든 커밋 메시지 끝에 트레일러 추가 — `-m "<title>" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`.

---

## File Structure

**생성:**
- `imstargg-admin-frontend/components/pending-image-input.tsx` — 생성 폼용 "보관형" 파일 선택기(부모가 File 보유, 미리보기 표시). Task 1.
- `imstargg-admin-frontend/app/brawlers/new/page.tsx` — 브롤러 전용 등록 페이지. Task 4.

**수정:**
- `imstargg-admin-frontend/lib/api.ts` — `createGear`(T1)/`createEventMode`(T2)/`createEvent`(T3)/`createBrawler`(T4) 시그니처 확장.
- `imstargg-admin-frontend/app/gears/page.tsx` — 등록 다이얼로그 확장(이름+이미지). Task 1.
- `imstargg-admin-frontend/app/event-modes/page.tsx` — 등록 다이얼로그 확장(이름+인원수+색상+아이콘). Task 2.
- `imstargg-admin-frontend/app/events/page.tsx` — 등록 다이얼로그 확장(맵 이름). Task 3.
- `imstargg-admin-frontend/app/brawlers/page.tsx` — 등록 다이얼로그 제거 → `/brawlers/new` 링크로 교체. Task 4.

**변경 없음:** `lib/types.ts`(요청 타입은 `lib/api.ts` 내 인라인 객체 타입으로 유지 — 기존 관행), 상세/편집 페이지, 기존 `ImageUploadField`.

---

## Task 1: 공유 PendingImageInput + 기어 등록 통합

**Files:**
- Create: `imstargg-admin-frontend/components/pending-image-input.tsx`
- Modify: `imstargg-admin-frontend/lib/api.ts` (`createGear`, 169-170행)
- Modify: `imstargg-admin-frontend/app/gears/page.tsx` (전체 교체)

- [ ] **Step 1: 공유 컴포넌트 생성** — `components/pending-image-input.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { Input } from "@/components/ui/input";

interface PendingImageInputProps {
  file: File | null;
  onChange: (file: File | null) => void;
  accept?: string;
}

export function PendingImageInput({ file, onChange, accept = "image/*" }: PendingImageInputProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  return (
    <div className="space-y-2">
      <Input type="file" accept={accept} onChange={(e) => onChange(e.target.files?.[0] ?? null)} />
      {previewUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={previewUrl} alt="" className="max-h-32" />
      )}
      {file && <p className="text-xs text-muted-foreground">선택됨: {file.name}</p>}
    </div>
  );
}
```

- [ ] **Step 2: createGear 시그니처 확장** — `lib/api.ts` 의 `createGear`(169-170행)를 교체:

```ts
export const createGear = (data: {
  brawlStarsId: number;
  rarity: string;
  names: Record<string, string>;
}) => request<void>("/gears", { method: "POST", body: JSON.stringify(data) });
```

- [ ] **Step 3: 기어 등록 폼 개편** — `app/gears/page.tsx` 전체를 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getGears, createGear, uploadGearImage } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import type { Gear } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const GEAR_RARITIES = ["SUPER_RARE", "EPIC", "MYTHIC"];

export default function GearListPage() {
  const [gears, setGears] = useState<Gear[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", ko: "", en: "" });
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadGears = () => getGears().then(setGears);

  useEffect(() => {
    loadGears();
  }, []);

  const resetForm = () => {
    setForm({ brawlStarsId: "", rarity: "", ko: "", en: "" });
    setImageFile(null);
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity) {
      toast.error("ID와 rarity를 입력해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createGear({
        brawlStarsId: id,
        rarity: form.rarity,
        names: { KOREAN: form.ko, ENGLISH: form.en },
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    if (imageFile) {
      try {
        await uploadGearImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 이미지 업로드에 실패했습니다. 편집에서 다시 업로드하세요.");
      }
    }
    toast.success("기어가 등록되었습니다.");
    setSubmitting(false);
    setOpen(false);
    resetForm();
    loadGears();
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">기어 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>기어 등록</DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>기어 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>Rarity</Label>
                <Select value={form.rarity} onValueChange={(v) => setForm({ ...form, rarity: v ?? "" })}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {GEAR_RARITIES.map((r) => (
                      <SelectItem key={r} value={r}>{r}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>이름 (KO)</Label>
                  <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
                </div>
                <div>
                  <Label>이름 (EN)</Label>
                  <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
                </div>
              </div>
              <div>
                <Label>이미지 (선택)</Label>
                <PendingImageInput file={imageFile} onChange={setImageFile} />
              </div>
              <Button onClick={handleCreate} className="w-full" disabled={submitting}>
                {submitting ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {gears.map((g) => (
            <TableRow key={g.brawlStarsId}>
              <TableCell>{g.brawlStarsId}</TableCell>
              <TableCell>{g.names.KOREAN || "-"}</TableCell>
              <TableCell>{g.names.ENGLISH || "-"}</TableCell>
              <TableCell>{g.rarity}</TableCell>
              <TableCell><ImageThumbnail url={g.imageUrl} alt={g.names.KOREAN || String(g.brawlStarsId)} /></TableCell>
              <TableCell>
                <Link href={`/gears/${g.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 4: 검증** — Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint`
Expected: 둘 다 에러 없이 종료(출력 없음 = 성공).

- [ ] **Step 5: 커밋**

```bash
cd imstargg-admin-frontend
git add components/pending-image-input.tsx lib/api.ts app/gears/page.tsx
git commit -m "feat(admin-frontend): 기어 등록 폼에 이름·이미지 통합 + PendingImageInput" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 이벤트 모드 등록 통합 (이름 + 인원수 + 색상 + 아이콘)

**Files:**
- Modify: `imstargg-admin-frontend/lib/api.ts` (`createEventMode`, 199-203행)
- Modify: `imstargg-admin-frontend/app/event-modes/page.tsx` (전체 교체)

- [ ] **Step 1: createEventMode 시그니처 확장** — `lib/api.ts` 의 `createEventMode`(199-203행)를 교체:

```ts
export const createEventMode = (data: {
  brawlStarsId: number;
  brawlStarsName: string;
  resultType?: BattleEventModeResultType;
  names: Record<string, string>;
  playerCounts: number[];
  color: string;
}) => request<void>("/event-modes", { method: "POST", body: JSON.stringify(data) });
```

- [ ] **Step 2: 이벤트 모드 등록 폼 개편** — `app/event-modes/page.tsx` 전체를 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getEventModes, createEventMode, uploadEventModeImage } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import { PendingImageInput } from "@/components/pending-image-input";
import type { BattleEventMode, BattleEventModeResultType } from "@/lib/types";
import { BATTLE_EVENT_MODE_RESULT_TYPES } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

const DEFAULT_COLOR = "#2A2E38";

export default function EventModeListPage() {
  const [eventModes, setEventModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<{
    brawlStarsId: string;
    brawlStarsName: string;
    resultType: BattleEventModeResultType;
    ko: string;
    en: string;
    playerCounts: string;
    color: string;
  }>({
    brawlStarsId: "",
    brawlStarsName: "",
    resultType: "RESULT",
    ko: "",
    en: "",
    playerCounts: "3,3",
    color: DEFAULT_COLOR,
  });
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadEventModes = () => getEventModes().then(setEventModes);

  useEffect(() => {
    loadEventModes();
  }, []);

  const resetForm = () => {
    setForm({
      brawlStarsId: "",
      brawlStarsName: "",
      resultType: "RESULT",
      ko: "",
      en: "",
      playerCounts: "3,3",
      color: DEFAULT_COLOR,
    });
    setImageFile(null);
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.brawlStarsName) {
      toast.error("ID와 이름(brawlStarsName)을 입력해주세요.");
      return;
    }
    const playerCounts = form.playerCounts.split(",").map((s) => Number(s.trim()));
    if (playerCounts.length === 0 || playerCounts.some((n) => !Number.isInteger(n) || n < 1)) {
      toast.error("플레이어 인원수는 1 이상의 정수를 콤마로 구분해 입력하세요. 예: 3,3");
      return;
    }
    setSubmitting(true);
    try {
      await createEventMode({
        brawlStarsId: id,
        brawlStarsName: form.brawlStarsName,
        resultType: form.resultType,
        names: { KOREAN: form.ko, ENGLISH: form.en },
        playerCounts,
        color: form.color,
      });
    } catch {
      toast.error("등록에 실패했습니다. (색상은 #RRGGBB 형식이어야 합니다)");
      setSubmitting(false);
      return;
    }
    if (imageFile) {
      try {
        await uploadEventModeImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 아이콘 업로드에 실패했습니다. 편집에서 다시 업로드하세요.");
      }
    }
    toast.success("이벤트 모드가 등록되었습니다.");
    setSubmitting(false);
    setOpen(false);
    resetForm();
    loadEventModes();
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 모드 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>이벤트 모드 등록</DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>이벤트 모드 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>BrawlStars ID</Label>
                  <Input
                    type="number"
                    value={form.brawlStarsId}
                    onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                  />
                </div>
                <div>
                  <Label>BrawlStars Name</Label>
                  <Input
                    value={form.brawlStarsName}
                    onChange={(e) => setForm({ ...form, brawlStarsName: e.target.value })}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>Result Type</Label>
                  <Select
                    value={form.resultType}
                    onValueChange={(v) =>
                      setForm({ ...form, resultType: (v ?? "RESULT") as BattleEventModeResultType })
                    }
                  >
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {BATTLE_EVENT_MODE_RESULT_TYPES.map((r) => (
                        <SelectItem key={r} value={r}>{r}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label>플레이어 인원수 (콤마 구분)</Label>
                  <Input
                    value={form.playerCounts}
                    onChange={(e) => setForm({ ...form, playerCounts: e.target.value })}
                    placeholder="예: 3,3"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>이름 (KO)</Label>
                  <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
                </div>
                <div>
                  <Label>이름 (EN)</Label>
                  <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
                </div>
              </div>
              <div>
                <Label>색상</Label>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={form.color}
                    onChange={(e) => setForm({ ...form, color: e.target.value })}
                    className="h-9 w-12 rounded border"
                  />
                  <Input value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} />
                </div>
              </div>
              <div>
                <Label>아이콘 (선택)</Label>
                <PendingImageInput file={imageFile} onChange={setImageFile} />
              </div>
              <Button onClick={handleCreate} className="w-full" disabled={submitting}>
                {submitting ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>BrawlStars Name</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>결과 타입</TableHead>
            <TableHead>아이콘</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {eventModes.map((m) => (
            <TableRow key={m.brawlStarsId}>
              <TableCell>{m.brawlStarsId}</TableCell>
              <TableCell>{m.brawlStarsName}</TableCell>
              <TableCell>{m.names.KOREAN || "-"}</TableCell>
              <TableCell>{m.names.ENGLISH || "-"}</TableCell>
              <TableCell>{m.resultType}</TableCell>
              <TableCell><ImageThumbnail url={m.iconImageUrl} alt={m.names.KOREAN || m.brawlStarsName} /></TableCell>
              <TableCell>
                <Link href={`/event-modes/${m.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 3: 검증** — Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint` → Expected: 에러 없음.

- [ ] **Step 4: 커밋**

```bash
cd imstargg-admin-frontend
git add lib/api.ts app/event-modes/page.tsx
git commit -m "feat(admin-frontend): 이벤트 모드 등록 폼에 이름·인원수·색상·아이콘 통합" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 이벤트 등록 통합 (맵 이름)

**Files:**
- Modify: `imstargg-admin-frontend/lib/api.ts` (`createEvent`, 187-188행)
- Modify: `imstargg-admin-frontend/app/events/page.tsx` (전체 교체)

- [ ] **Step 1: createEvent 시그니처 확장** — `lib/api.ts` 의 `createEvent`(187-188행)를 교체:

```ts
export const createEvent = (data: {
  brawlStarsId: number;
  modeId?: number;
  mapName?: string;
  mapNames: Record<string, string>;
}) => request<void>("/events", { method: "POST", body: JSON.stringify(data) });
```

- [ ] **Step 2: 이벤트 등록 폼 개편** — `app/events/page.tsx` 전체를 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getEvents, getEventModes, createEvent } from "@/lib/api";
import type { BattleEvent, BattleEventMode } from "@/lib/types";
import Link from "next/link";
import { toast } from "sonner";

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default function EventListPage() {
  const [events, setEvents] = useState<BattleEvent[]>([]);
  const [eventModes, setEventModes] = useState<BattleEventMode[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ brawlStarsId: "", modeId: "", mapName: "", ko: "", en: "" });
  const [submitting, setSubmitting] = useState(false);

  const loadEvents = () => getEvents().then(setEvents);

  useEffect(() => {
    loadEvents();
    getEventModes().then(setEventModes);
  }, []);

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id) {
      toast.error("BrawlStars ID를 입력해주세요.");
      return;
    }
    setSubmitting(true);
    try {
      await createEvent({
        brawlStarsId: id,
        modeId: form.modeId ? Number(form.modeId) : undefined,
        mapName: form.mapName || undefined,
        mapNames: { KOREAN: form.ko, ENGLISH: form.en },
      });
    } catch {
      toast.error("등록에 실패했습니다.");
      setSubmitting(false);
      return;
    }
    toast.success("이벤트가 등록되었습니다.");
    setSubmitting(false);
    setOpen(false);
    setForm({ brawlStarsId: "", modeId: "", mapName: "", ko: "", en: "" });
    loadEvents();
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">이벤트 관리</h2>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger render={<Button />}>이벤트 등록</DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>이벤트 등록</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label>BrawlStars ID</Label>
                <Input
                  type="number"
                  value={form.brawlStarsId}
                  onChange={(e) => setForm({ ...form, brawlStarsId: e.target.value })}
                />
              </div>
              <div>
                <Label>모드 (선택)</Label>
                <Select value={form.modeId} onValueChange={(v) => setForm({ ...form, modeId: v ?? "" })}>
                  <SelectTrigger><SelectValue placeholder="모드 선택" /></SelectTrigger>
                  <SelectContent>
                    {eventModes.map((m) => (
                      <SelectItem key={m.brawlStarsId} value={String(m.brawlStarsId)}>
                        {m.brawlStarsName} ({m.brawlStarsId})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>맵 이름 (선택)</Label>
                <Input
                  value={form.mapName}
                  onChange={(e) => setForm({ ...form, mapName: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>맵 이름 (KO)</Label>
                  <Input value={form.ko} onChange={(e) => setForm({ ...form, ko: e.target.value })} />
                </div>
                <div>
                  <Label>맵 이름 (EN)</Label>
                  <Input value={form.en} onChange={(e) => setForm({ ...form, en: e.target.value })} />
                </div>
              </div>
              <Button onClick={handleCreate} className="w-full" disabled={submitting}>
                {submitting ? "등록 중..." : "등록"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>모드</TableHead>
            <TableHead>맵 이름</TableHead>
            <TableHead>맵 이름 (KO)</TableHead>
            <TableHead>맵 이름 (EN)</TableHead>
            <TableHead>생성일</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {events.map((e) => (
            <TableRow key={e.brawlStarsId}>
              <TableCell>{e.brawlStarsId}</TableCell>
              <TableCell>{e.modeBrawlStarsName || "-"}</TableCell>
              <TableCell>{e.mapName || "-"}</TableCell>
              <TableCell>{e.mapNames.KOREAN || "-"}</TableCell>
              <TableCell>{e.mapNames.ENGLISH || "-"}</TableCell>
              <TableCell>{e.createdAt ? formatDateTime(e.createdAt) : "-"}</TableCell>
              <TableCell>
                <Link href={`/events/${e.brawlStarsId}/edit`}>
                  <Button variant="outline" size="sm">편집</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 3: 검증** — Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint` → Expected: 에러 없음.

- [ ] **Step 4: 커밋**

```bash
cd imstargg-admin-frontend
git add lib/api.ts app/events/page.tsx
git commit -m "feat(admin-frontend): 이벤트 등록 폼에 맵 이름 통합" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: 브롤러 등록 통합 (전용 페이지 + 기어 그리드)

**Files:**
- Modify: `imstargg-admin-frontend/lib/api.ts` (`createBrawler`, 40-41행)
- Create: `imstargg-admin-frontend/app/brawlers/new/page.tsx`
- Modify: `imstargg-admin-frontend/app/brawlers/page.tsx` (등록 다이얼로그 제거 → 링크)

- [ ] **Step 1: createBrawler 시그니처 확장** — `lib/api.ts` 의 `createBrawler`(40-41행)를 교체:

```ts
export const createBrawler = (data: {
  brawlStarsId: number;
  rarity: string;
  role: string;
  names: Record<string, string>;
  gearBrawlStarsIds: number[];
}) => request<void>("/brawlers", { method: "POST", body: JSON.stringify(data) });
```

- [ ] **Step 2: 브롤러 전용 등록 페이지 생성** — `app/brawlers/new/page.tsx`:

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
import { getGears, createBrawler, uploadBrawlerImage } from "@/lib/api";
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

export default function BrawlerCreatePage() {
  const router = useRouter();
  const [gears, setGears] = useState<Gear[]>([]);
  const [form, setForm] = useState({ brawlStarsId: "", rarity: "", role: "", ko: "", en: "" });
  const [selectedGearIds, setSelectedGearIds] = useState<number[]>([]);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getGears().then(setGears);
  }, []);

  const toggleGear = (id: number) => {
    setSelectedGearIds((prev) =>
      prev.includes(id) ? prev.filter((g) => g !== id) : [...prev, id],
    );
  };

  const handleCreate = async () => {
    const id = Number(form.brawlStarsId);
    if (!id || !form.rarity || !form.role) {
      toast.error("ID, rarity, role을 입력해주세요.");
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
    if (imageFile) {
      try {
        await uploadBrawlerImage(id, imageFile);
      } catch {
        toast.error("등록은 됐지만 이미지 업로드에 실패했습니다. 상세에서 다시 업로드하세요.");
      }
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

- [ ] **Step 3: 브롤러 목록 페이지의 등록 다이얼로그 → 링크 교체** — `app/brawlers/page.tsx` 전체를 교체:

```tsx
"use client";

import { useEffect, useState } from "react";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { getBrawlers } from "@/lib/api";
import { ImageThumbnail } from "@/components/image-thumbnail";
import type { BrawlerListItem } from "@/lib/types";
import Link from "next/link";

export default function BrawlerListPage() {
  const [brawlers, setBrawlers] = useState<BrawlerListItem[]>([]);

  useEffect(() => {
    getBrawlers().then(setBrawlers);
  }, []);

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">브롤러 관리</h2>
        <Link href="/brawlers/new">
          <Button>브롤러 등록</Button>
        </Link>
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>BrawlStars ID</TableHead>
            <TableHead>이름 (KO)</TableHead>
            <TableHead>이름 (EN)</TableHead>
            <TableHead>Rarity</TableHead>
            <TableHead>Role</TableHead>
            <TableHead>이미지</TableHead>
            <TableHead></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {brawlers.map((b) => (
            <TableRow key={b.brawlStarsId}>
              <TableCell>{b.brawlStarsId}</TableCell>
              <TableCell>{b.names.KOREAN || "-"}</TableCell>
              <TableCell>{b.names.ENGLISH || "-"}</TableCell>
              <TableCell>{b.rarity}</TableCell>
              <TableCell>{b.role}</TableCell>
              <TableCell><ImageThumbnail url={b.imageUrl} alt={b.names.KOREAN || String(b.brawlStarsId)} /></TableCell>
              <TableCell>
                <Link href={`/brawlers/${b.brawlStarsId}`}>
                  <Button variant="outline" size="sm">상세</Button>
                </Link>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </AdminLayout>
  );
}
```

- [ ] **Step 4: 검증** — Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint` → Expected: 에러 없음.

- [ ] **Step 5: 커밋**

```bash
cd imstargg-admin-frontend
git add lib/api.ts app/brawlers/new/page.tsx app/brawlers/page.tsx
git commit -m "feat(admin-frontend): 브롤러 전용 등록 페이지(/brawlers/new) + 기어 그리드·이미지 통합" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: 최종 검증 (빌드 + 수동 스모크)

**Files:** 없음(검증만).

- [ ] **Step 1: 타입체크 + 린트 + 프로덕션 빌드**

Run: `cd imstargg-admin-frontend && npx tsc --noEmit && pnpm lint && pnpm build`
Expected: 세 단계 모두 성공(`pnpm build` 가 컴파일·타입·라우트 생성까지 통과). 실패 시 메시지를 읽고 해당 Task 로 돌아가 수정.

- [ ] **Step 2: 수동 스모크 (백엔드 연동) — 선택적, 사람이 수행 권장**

전제: 로컬 인프라(Postgres 등) 가동 중. 두 프로세스 실행:
```bash
# 터미널 A — 관리자 백엔드 (계획 1의 확장된 생성 API 포함)
cd imstargg-backend && ./gradlew :core:core-admin:bootRun
# 터미널 B — 관리자 프론트
cd imstargg-admin-frontend && pnpm dev
```
브라우저에서 확인(기본 http://localhost:3000, API 기본 http://localhost:8080):
- 기어 등록: 이름(KO/EN) + 이미지 선택 후 등록 → 목록에 이름/이미지 반영.
- 이벤트 모드 등록: 이름·인원수(3,3)·색상·아이콘 입력 후 등록 → 목록 반영. 색상에 잘못된 값(예: `xyz`) 입력 시 등록 실패 토스트.
- 이벤트 등록: 맵 이름(KO/EN) 입력 후 등록 → 목록의 맵 이름(KO/EN) 반영.
- 브롤러 등록: `/brawlers/new` 에서 이름·역할·기어 토글·이미지 입력 후 등록 → 상세로 이동, 데이터 반영.
- 이미지만 실패하는 경우(예: 잘못된 파일) 엔티티는 생성되고 "이미지 업로드 실패" 토스트가 뜨는지.

> 참고: `pnpm dev` 의 `NEXT_PUBLIC_API_URL` 미설정 시 기본 `http://localhost:8080` 로 호출. 필요 시 환경변수로 조정.

---

## 참고 — 후속 계획

- **계획 3(브롤러 상세 단일 페이지)** 은 프론트 전용·독립적이며 본 계획과 무관하게 진행 가능. 탭 제거 + 섹션 순서(이름→기어→가젯→스타파워→하이퍼차지→버피) + 기어 이미지 그리드.
- 본 계획은 계획 1에서 확장한 생성 API 에 의존한다(이미 구현·머지 대기 상태). 백엔드가 같은 브랜치에 있으므로 `pnpm dev` + `bootRun` 으로 함께 동작 확인 가능.
