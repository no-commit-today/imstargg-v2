"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/", label: "대시보드" },
  { href: "/brawlers", label: "브롤러 관리" },
  { href: "/gears", label: "기어 관리" },
  { href: "/events", label: "이벤트 관리" },
  { href: "/event-modes", label: "이벤트 모드 관리" },
  { href: "/maintenances", label: "API 점검 관리" },
  { href: "/api-specs", label: "API 스펙 추적" },
  { href: "/feedbacks", label: "피드백" },
];

export function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex h-screen">
      <aside className="w-64 border-r bg-muted/40 p-4">
        <h1 className="mb-6 text-lg font-semibold">imstargg Admin</h1>
        <nav className="space-y-1">
          {navItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "block rounded-md px-3 py-2 text-sm",
                (item.href === "/" ? pathname === "/" : pathname.startsWith(item.href))
                  ? "bg-primary text-primary-foreground"
                  : "hover:bg-muted"
              )}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
