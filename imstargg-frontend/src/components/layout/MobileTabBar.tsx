"use client";
import { useRouter, usePathname } from "next/navigation";
import { Calendar } from "lucide-react";
import type { ReactNode } from "react";

interface Tab {
  route: string;
  label: string;
  icon: ReactNode;
  match?: (r: string) => boolean;
}

const ASSET_ICON = (src: string) => (
  <img src={src} alt="" width={20} height={20} />
);

const TABS: Tab[] = [
  {
    route: "/",
    label: "홈",
    icon: ASSET_ICON("/assets/star.webp"),
    match: (r) => r === "/" || r.startsWith("/players/"),
  },
  { route: "/brawlers", label: "브롤러", icon: ASSET_ICON("/assets/star_wings.webp") },
  { route: "/events", label: "이벤트", icon: <Calendar size={20} strokeWidth={2.25} aria-hidden /> },
  { route: "/ranking", label: "랭킹", icon: ASSET_ICON("/assets/trophy.png") },
  { route: "/clubs", label: "클럽", icon: ASSET_ICON("/assets/club_league.png") },
];

export default function MobileTabBar() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const isActive = (prefix: string) => pathname === prefix || pathname.startsWith(prefix + "/");
  return (
    <div className="mobile-tabbar">
      {TABS.map((t) => {
        const active = t.match ? t.match(pathname) : isActive(t.route);
        return (
          <a
            key={t.route}
            className={active ? "active" : ""}
            onClick={() => router.push(t.route)}
            style={{ cursor: "pointer" }}
          >
            <span className="tab-icon">{t.icon}</span>
            <span>{t.label}</span>
          </a>
        );
      })}
    </div>
  );
}
