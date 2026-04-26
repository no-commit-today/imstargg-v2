import PlayerSidebar from "./_components/PlayerSidebar";
import PlayerContent from "./_components/PlayerContent";

interface PageProps {
  params: Promise<{ tag: string }>;
  searchParams: Promise<{ tab?: string }>;
}

export default async function Page({ searchParams }: PageProps) {
  const sp = await searchParams;
  const tab = (sp?.tab as "battles" | "brawlers" | "stats") || "battles";
  return (
    <div className="main" style={{ display: "flex", gap: 16, alignItems: "flex-start" }}>
      <PlayerSidebar />
      <PlayerContent activeTab={tab} />
    </div>
  );
}
