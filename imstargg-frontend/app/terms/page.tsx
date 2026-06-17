/*
 * 이용약관 — 표준 항목을 포함한 초안입니다.
 * 법적 효력이 있는 문서이므로, 실제 서비스 운영 형태에 맞게 운영자가 최종
 * 검토·수정한 뒤 공개해야 합니다.
 */
import type { Metadata } from "next";
import { siteConfig } from "@/config/site";
import { buildMetadata } from "@/lib/seo";
import { LegalSection } from "../_components/LegalSection";

export const metadata: Metadata = buildMetadata({
  title: "이용약관 | ImStarGG",
  description:
    "ImStarGG 서비스 이용에 관한 약관과 면책 사항, 저작권 및 금지 행위 안내입니다.",
  path: "/terms",
});

export default function TermsPage() {
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-6">
      <header className="space-y-1">
        <h1 className="font-display text-3xl tracking-[0.02em]">이용약관</h1>
        <p className="text-sm text-[#5C6271]">
          ImStarGG 서비스 이용 조건을 안내합니다.
        </p>
      </header>

      <div className="max-w-3xl space-y-8">
        <LegalSection title="서비스 소개">
          <p>
            본 약관은 ImStarGG(이하 &ldquo;사이트&rdquo;)가 제공하는 브롤스타즈
            통계 조회 서비스의 이용 조건과 절차, 이용자와 사이트의 권리·의무를
            규정합니다. 이용자가 사이트를 이용함으로써 본 약관에 동의한 것으로
            간주합니다.
          </p>
        </LegalSection>

        <LegalSection title="서비스의 제공 및 변경">
          <p>
            사이트는 브롤스타즈 공개 API를 기반으로 한 전적·통계·랭킹·이벤트
            정보를 제공합니다. 사이트는 서비스의 내용을 사전 고지 없이 변경하거나
            중단할 수 있으며, 무료로 제공되는 서비스의 특성상 운영상·기술상
            필요에 따라 일부 기능이 제한될 수 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="데이터의 정확성과 면책">
          <p>
            사이트가 제공하는 모든 정보는 Supercell의 공개 API에서 수집한 데이터를
            가공한 것으로, 게임 데이터 반영 시점이나 외부 API 상태에 따라 실제
            게임 내 정보와 차이가 있을 수 있습니다. 사이트는 정보의
            정확성·완전성·적시성을 보장하지 않으며, 이용자가 본 정보를 신뢰하여
            내린 판단과 그 결과에 대해 책임지지 않습니다.
          </p>
        </LegalSection>

        <LegalSection title="저작권 및 팬 콘텐츠 정책">
          <p>
            브롤스타즈(Brawl Stars) 및 관련 명칭, 로고, 캐릭터, 이미지 등 게임
            관련 지식재산권은 모두 Supercell에 귀속됩니다. ImStarGG는 Supercell과
            무관한 비공식 팬 사이트이며, Supercell의{" "}
            <a
              href="https://www.supercell.com/fan-content-policy"
              target="_blank"
              rel="noopener noreferrer"
            >
              팬 콘텐츠 정책
            </a>
            을 준수합니다. 사이트가 직접 제작한 디자인·텍스트·코드에 대한 권리는
            사이트 운영자에게 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="금지 행위">
          <p>이용자는 다음 행위를 해서는 안 됩니다.</p>
          <ul>
            <li>
              자동화된 수단으로 사이트에 과도한 부하를 유발하거나 데이터를
              무단으로 대량 수집(크롤링·스크래핑)하는 행위
            </li>
            <li>
              사이트의 정상적인 운영을 방해하거나 시스템에 무단으로 접근하려는
              행위
            </li>
            <li>사이트의 콘텐츠를 무단으로 상업적 목적에 이용하는 행위</li>
            <li>
              관련 법령 또는 본 약관, Supercell의 팬 콘텐츠 정책을 위반하는 행위
            </li>
          </ul>
        </LegalSection>

        <LegalSection title="약관의 변경">
          <p>
            본 약관은 필요 시 개정될 수 있으며, 변경된 약관은 본 페이지에
            게시함으로써 효력이 발생합니다.
          </p>
        </LegalSection>

        <LegalSection title="문의">
          <p>
            약관 관련 문의:{" "}
            <a href={`mailto:${siteConfig.email}`}>{siteConfig.email}</a>
          </p>
        </LegalSection>
      </div>
    </main>
  );
}
