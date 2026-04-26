"use client";
import { useRouter, usePathname } from "next/navigation";
import PlayerBrawlers from "./PlayerBrawlers";
import PlayerBattles from "./PlayerBattles";
import PlayerStats from "./PlayerStats";

interface Props {
  activeTab: "battles" | "brawlers" | "stats";
}

const TABS = [
  { id: "battles", label: "전투 기록" },
  { id: "brawlers", label: "보유 브롤러" },
  { id: "stats", label: "통계" },
] as const;

export default function PlayerContent({ activeTab }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const setActive = (id: string) => {
    router.push(`${pathname}?tab=${id}`);
  };

  return (
    <main style={{ flex: 1, minWidth: 0 }}>
      <div className="tabs" style={{ marginBottom: 16 }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab ${activeTab === t.id ? "active" : ""}`}
            onClick={() => setActive(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {activeTab === "brawlers" && <PlayerBrawlers />}
      {activeTab === "battles" && <PlayerBattles />}
      {activeTab === "stats" && <PlayerStats />}
    </main>
  );
}
