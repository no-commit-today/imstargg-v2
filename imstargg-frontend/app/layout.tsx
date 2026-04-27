import type { Metadata } from "next";
import "./globals.css";
import TopNav from "@/components/layout/TopNav";
import MobileTabBar from "@/components/layout/MobileTabBar";
import { Toaster } from "@/components/ui/sonner";
import { QueryProvider } from "@/providers/QueryProvider";

export const metadata: Metadata = {
  title: "ImStarGG — 브롤스타즈 전적 통계",
  description: "브롤스타즈 전적 · 통계 · 메타 검색",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <QueryProvider>
          <div className="app">
            <TopNav />
            {children}
            <MobileTabBar />
            <Toaster richColors position="top-center" />
          </div>
        </QueryProvider>
      </body>
    </html>
  );
}
