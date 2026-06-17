import Image from "next/image";
import Link from "next/link";
import { siteConfig } from "@/config/site";
import { FeedbackFooterLink } from "@/components/layout/FeedbackFooterLink";

export default function SiteFooter() {
  return (
    <footer className="bg-neutral-100 py-6 sm:py-10 mt-6 sm:mt-10">
      <div className="max-w-7xl mx-auto px-4 space-y-4">
        <div className="flex items-center">
          <Image
            src={siteConfig.logo}
            alt={siteConfig.name}
            width={102}
            height={24}
            priority={false}
          />
        </div>
        <nav className="flex flex-wrap gap-x-4 gap-y-1 text-neutral-500 text-xs">
          <Link
            href="/about"
            className="hover:text-neutral-700 underline-offset-2 hover:underline"
          >
            소개
          </Link>
          <Link
            href="/terms"
            className="hover:text-neutral-700 underline-offset-2 hover:underline"
          >
            이용약관
          </Link>
          <Link
            href="/privacy"
            className="hover:text-neutral-700 underline-offset-2 hover:underline"
          >
            개인정보처리방침
          </Link>
        </nav>
        <div className="text-neutral-500 text-xs">
          <p>이메일: {siteConfig.email}</p>
          <p className="mt-1">
            <FeedbackFooterLink />
          </p>
        </div>
        <div className="text-neutral-500 text-xs space-y-1">
          <p>ImStarGG는 Supercell과 무관한 비공식 팬 사이트입니다.</p>
          <p>
            This content is not affiliated with, endorsed, sponsored, or
            specifically approved by Supercell and Supercell is not responsible
            for it. For more information see{" "}
            <Link
              href="https://www.supercell.com/fan-content-policy"
              target="_blank"
              rel="noopener noreferrer"
              className="underline"
            >
              Supercell&apos;s Fan Content Policy
            </Link>
            .
          </p>
        </div>
      </div>
    </footer>
  );
}
