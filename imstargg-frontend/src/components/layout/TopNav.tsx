"use client";
import { useState } from "react";
import Image from "next/image";
import { Menu, MessageSquare, X } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";

interface NavItem {
  href: string;
  label: string;
}

const NAV_ITEMS: NavItem[] = [
  { href: "/brawlers", label: "브롤러" },
  { href: "/events", label: "이벤트" },
  { href: "/players", label: "플레이어" },
  { href: "/clubs", label: "클럽" },
];

export default function TopNav() {
  const router = useRouter();
  const pathname = usePathname() || "/";
  const [drawerOpen, setDrawerOpen] = useState(false);

  const navigate = (href: string) => {
    router.push(href);
    setDrawerOpen(false);
  };

  const { setOpen: setFeedbackOpen } = useFeedbackDialog();

  const isActive = (prefix: string) =>
    pathname === prefix || pathname.startsWith(prefix + "/");

  return (
    <nav className="topnav">
      <div className="topnav-inner">
        <button
          type="button"
          className="topnav-logo"
          onClick={() => router.push("/")}
          title="ImStarGG"
        >
          <Image
            src="/assets/logo.webp"
            alt="ImStarGG"
            width={119}
            height={28}
            unoptimized
          />
        </button>
        <div className="topnav-links">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.href}
              type="button"
              className={`topnav-link ${isActive(item.href) ? "active" : ""}`}
              onClick={() => router.push(item.href)}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="topnav-spacer" />

        <button
          type="button"
          onClick={() => setFeedbackOpen(true)}
          className="topnav-link"
          aria-label="의견 보내기"
        >
          <MessageSquare className="h-4 w-4" aria-hidden="true" />
          <span className="hidden sm:inline">피드백</span>
        </button>

        <div className="topnav-desktop-search">
          <PlayerSearchInput variant="topnav" />
        </div>

        <Sheet open={drawerOpen} onOpenChange={setDrawerOpen}>
          <button
            type="button"
            className="topnav-menu-button"
            aria-label="모바일 메뉴 열기"
            onClick={() => setDrawerOpen(true)}
          >
            <Menu aria-hidden="true" />
          </button>
          <SheetContent
            side="right"
            className="topnav-drawer"
            showCloseButton={false}
          >
            <SheetHeader className="topnav-drawer-header">
              <div>
                <SheetTitle>메뉴</SheetTitle>
                <SheetDescription className="sr-only">
                  주요 페이지와 플레이어 검색
                </SheetDescription>
              </div>
              <button
                type="button"
                className="topnav-drawer-close"
                aria-label="모바일 메뉴 닫기"
                onClick={() => setDrawerOpen(false)}
              >
                <X aria-hidden="true" />
              </button>
            </SheetHeader>
            <div className="topnav-search-drawer">
              <PlayerSearchInput />
            </div>
            <div className="topnav-drawer-links">
              {NAV_ITEMS.map((item) => (
                <button
                  key={item.href}
                  type="button"
                  className={isActive(item.href) ? "active" : ""}
                  aria-label={`${item.label} 페이지로 이동`}
                  onClick={() => navigate(item.href)}
                >
                  {item.label}
                </button>
              ))}
              <button
                type="button"
                aria-label="의견 보내기"
                onClick={() => {
                  setDrawerOpen(false);
                  setFeedbackOpen(true);
                }}
              >
                피드백
              </button>
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </nav>
  );
}
