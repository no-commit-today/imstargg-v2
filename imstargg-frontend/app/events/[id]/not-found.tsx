import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function EventNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">이벤트를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">존재하지 않는 ID일 수 있어요.</p>
      <Button asChild variant="outline">
        <Link href="/events">이벤트 목록</Link>
      </Button>
    </main>
  );
}
