import type { Metadata } from "next";
import "./globals.css";
import TopNav from "@/components/layout/TopNav";
import MobileTabBar from "@/components/layout/MobileTabBar";
import { Toaster } from "@/components/ui/sonner";
import { QueryProvider } from "@/providers/QueryProvider";
import { env } from "@/config/env";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = {
  metadataBase: new URL(env.NEXT_PUBLIC_SITE_URL),
  ...buildMetadata({
    title: "ImStarGG — 브롤스타즈 통계",
    description: "브롤스타즈 플레이어/클럽/브롤러/이벤트 통계와 랭킹.",
    path: "/",
  }),
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
