import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function PlayerNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">플레이어를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">태그가 정확한지 다시 확인해 주세요.</p>
      <Button asChild variant="outline">
        <Link href="/">홈으로</Link>
      </Button>
    </main>
  );
}
