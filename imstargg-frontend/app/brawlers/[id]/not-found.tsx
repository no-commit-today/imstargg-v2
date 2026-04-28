import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function BrawlerNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">브롤러를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">존재하지 않는 ID일 수 있어요.</p>
      <Button asChild variant="outline">
        <Link href="/brawlers">브롤러 도감</Link>
      </Button>
    </main>
  );
}
