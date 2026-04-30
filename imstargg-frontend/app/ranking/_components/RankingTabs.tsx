"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/ranking/players", label: "플레이어" },
  { href: "/ranking/brawlers", label: "브롤러" },
  { href: "/ranking/clubs", label: "클럽" },
] as const;

export function RankingTabs() {
  const pathname = usePathname();
  return (
    <nav className="-mx-4 flex gap-1 overflow-x-auto border-b border-[#D8DCE5] px-4 sm:mx-0 sm:px-0">
      {TABS.map((tab) => {
        const active = pathname === tab.href || pathname.startsWith(`${tab.href}/`);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`relative whitespace-nowrap px-3 py-2.5 text-sm font-bold tracking-wide transition-colors sm:px-4 ${
              active
                ? "text-[#14161B] after:absolute after:inset-x-3 after:bottom-[-1px] after:h-0.5 after:bg-[#FFC60A] sm:after:inset-x-4"
                : "text-[#5C6271] hover:text-[#14161B]"
            }`}
          >
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}
