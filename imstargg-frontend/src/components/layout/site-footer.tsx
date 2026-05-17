import Image from "next/image";
import Link from "next/link";
import { siteConfig } from "@/config/site";

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
        <div className="text-neutral-500 text-xs">
          <p>이메일: {siteConfig.email}</p>
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
