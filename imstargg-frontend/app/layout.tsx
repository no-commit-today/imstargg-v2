import type { Metadata } from "next";
import "./globals.css";
import TopNav from "@/components/layout/TopNav";
import MobileTabBar from "@/components/layout/MobileTabBar";
import ToastContainer from "@/components/shared/ToastContainer";

export const metadata: Metadata = {
  title: "ImStarGG — 브롤스타즈 전적 통계",
  description: "브롤스타즈 전적 · 통계 · 메타 검색",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <div className="app">
          <TopNav />
          {children}
          <MobileTabBar />
          <ToastContainer />
        </div>
      </body>
    </html>
  );
}
