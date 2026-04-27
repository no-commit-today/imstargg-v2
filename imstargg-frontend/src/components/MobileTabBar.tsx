"use client";
import { useRouter, usePathname } from "next/navigation";

interface Tab {
  route: string;
  label: string;
  icon: string;
  match?: (r: string) => boolean;
}

const TABS: Tab[] = [
  { route: "/", label: "홈", icon: "🏠", match: (r) => r === "/" || r.startsWith("/players/") },
  { route: "/brawlers", label: "브롤러", icon: "⭐" },
  { route: "/events", label: "이벤트", icon: "🗓️" },
  { route: "/ranking", label: "랭킹", icon: "🏆" },
  { route: "/clubs", label: "클럽", icon: "🛡️" },
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
