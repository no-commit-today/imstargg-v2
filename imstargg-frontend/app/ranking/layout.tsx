import { RankingTabs } from "./_components/RankingTabs";

export default function RankingLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <main className="container mx-auto max-w-7xl space-y-4 px-4 py-8">
      <RankingTabs />
      {children}
    </main>
  );
}
