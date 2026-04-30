import type { Metadata } from "next";
import Link from "next/link";
import { brawlerApi } from "@/api/endpoints/brawler";
import { toBrawler } from "@/domain/brawler/mappers";
import { sortByName } from "@/domain/brawler/filters";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import { Card } from "@/components/ui/card";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "브롤러 랭킹 | ImStarGG",
  description: "브롤러를 선택하여 국가별 랭킹을 확인하세요.",
  path: "/ranking/brawlers",
});

export const revalidate = 86400;

export default async function Page() {
  const list = await brawlerApi.list({ next: { revalidate: 86400 } });
  const brawlers = sortByName(list.content.map(toBrawler));

  return (
    <main className="container mx-auto max-w-7xl space-y-4 px-4 py-8">
      <h1 className="text-2xl font-bold">브롤러 랭킹</h1>
      <p className="text-sm text-muted-foreground">
        랭킹을 확인할 브롤러를 선택하세요.
      </p>
      <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {brawlers.map((b) => (
          <Link
            key={b.id}
            href={`/ranking/brawlers/${b.id}`}
            className="block"
          >
            <Card className="flex flex-col items-center gap-2 p-3 transition hover:border-foreground">
              <BrawlerAvatar
                name={b.name}
                imageUrl={b.portraitImageUrl}
                rarity={b.rarity}
                size={72}
                showFrame
              />
              <div className="w-full truncate text-center text-xs font-bold">
                {b.name}
              </div>
            </Card>
          </Link>
        ))}
      </div>
    </main>
  );
}
