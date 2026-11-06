"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  BRAWLER_META_VIEWS,
  BRAWLER_META_VIEW_LABELS,
  DEFAULT_BRAWLER_META_VIEW,
  type BrawlerMetaView,
} from "@/domain/statistics/enums";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

/** 브롤러 / 2인 조합 / 3인 조합 — 내용은 페이지가 서버에서 그리므로 TabsContent 가 없다. */
export function BrawlerMetaViewTabs({ view }: { view: BrawlerMetaView }) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function handleChange(next: string) {
    const sp = new URLSearchParams(params);
    if (next === DEFAULT_BRAWLER_META_VIEW) sp.delete("view");
    else sp.set("view", next);
    const qs = sp.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <Tabs value={view} onValueChange={handleChange} className="w-full">
      <TabsList
        variant="line"
        className="h-11 w-full justify-start gap-4 border-b border-[#D8DCE5] px-0"
      >
        {BRAWLER_META_VIEWS.map((v) => (
          <TabsTrigger
            key={v}
            value={v}
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            {BRAWLER_META_VIEW_LABELS[v]}
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  );
}
