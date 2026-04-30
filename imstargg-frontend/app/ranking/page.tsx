import type { Metadata } from "next";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "랭킹 | ImStarGG",
  description: "브롤스타즈 플레이어, 브롤러, 클럽 트로피 랭킹을 확인하세요.",
  path: "/ranking",
});

const ITEMS = [
  {
    href: "/ranking/players",
    title: "플레이어 랭킹",
    description: "국가별 플레이어 트로피 순위",
  },
  {
    href: "/ranking/brawlers",
    title: "브롤러 랭킹",
    description: "각 브롤러별 국가 트로피 순위",
  },
  {
    href: "/ranking/clubs",
    title: "클럽 랭킹",
    description: "국가별 클럽 트로피 순위",
  },
];

export default function Page() {
  return (
    <main className="container mx-auto max-w-7xl space-y-6 px-4 py-8">
      <h1 className="text-2xl font-bold">랭킹</h1>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {ITEMS.map((item) => (
          <Link key={item.href} href={item.href} className="block">
            <Card className="h-full p-5 transition hover:border-foreground">
              <h2 className="text-lg font-bold">{item.title}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {item.description}
              </p>
            </Card>
          </Link>
        ))}
      </div>
    </main>
  );
}
