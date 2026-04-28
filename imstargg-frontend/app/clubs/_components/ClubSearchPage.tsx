import { ClubSearchInput } from "./ClubSearchInput";

export default function ClubSearchPage() {
  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-14">
      <div className="mb-6">
        <div className="mb-2 text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          CLUBS
        </div>
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          클럽 태그로 멤버 · 트로피 정보를 조회하세요.
        </p>
      </div>
      <ClubSearchInput />
    </main>
  );
}
