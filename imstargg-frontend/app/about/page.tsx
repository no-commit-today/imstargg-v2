import type { Metadata } from "next";
import { siteConfig } from "@/config/site";
import { buildMetadata } from "@/lib/seo";
import { LegalSection } from "../_components/LegalSection";

export const metadata: Metadata = buildMetadata({
  title: "소개 | ImStarGG",
  description:
    "ImStarGG는 브롤스타즈 플레이어·클럽 전적과 브롤러 메타, 이벤트 정보를 제공하는 한국어 통계 사이트입니다.",
  path: "/about",
});

export default function AboutPage() {
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-6">
      <header className="space-y-1">
        <h1 className="font-display text-3xl tracking-[0.02em]">소개</h1>
        <p className="text-sm text-[#5C6271]">
          ImStarGG가 어떤 서비스인지 안내합니다.
        </p>
      </header>

      <div className="max-w-3xl space-y-8">
        <LegalSection title="ImStarGG란?">
          <p>
            ImStarGG는 인기 모바일 게임 브롤스타즈(Brawl Stars)의 플레이어 전적과
            통계를 한국어로 제공하는 비공식 팬 사이트입니다. 게임 내 플레이어
            태그만 입력하면 최근 전투 기록, 트로피 변화, 브롤러별 성적을 빠르게
            확인할 수 있습니다. 복잡한 데이터를 정돈된 표와 카드로 정리해, 누구나
            자신과 친구의 실력을 쉽게 분석하고 비교할 수 있도록 만드는 것을
            목표로 합니다.
          </p>
        </LegalSection>

        <LegalSection title="제공 기능">
          <ul>
            <li>
              <strong>플레이어 전적</strong> — 플레이어 태그 또는 이름으로 검색해
              최근 전투 결과, 승률, 사용 브롤러, 트로피 추이를 확인할 수
              있습니다.
            </li>
            <li>
              <strong>클럽 정보</strong> — 클럽을 검색해 소속 멤버와 클럽 전체
              트로피, 활동 현황을 살펴볼 수 있습니다.
            </li>
            <li>
              <strong>브롤러 메타</strong> — 티어 구간·기간별로 집계한 브롤러
              승률·픽률 통계를 제공해 현재 강세인 브롤러를 파악할 수 있습니다.
            </li>
            <li>
              <strong>이벤트 로테이션</strong> — 현재 진행 중인 게임 모드와 맵,
              경쟁전 정보를 정리해 보여줍니다.
            </li>
            <li>
              <strong>랭킹</strong> — 국가별 플레이어·클럽 트로피 랭킹을
              제공합니다.
            </li>
          </ul>
        </LegalSection>

        <LegalSection title="데이터 출처와 갱신">
          <p>
            ImStarGG가 제공하는 모든 통계는 Supercell이 공식 제공하는 브롤스타즈
            공개 API에서 수집한 데이터를 가공한 것입니다. 전적과 랭킹은 게임
            데이터가 갱신되는 주기에 맞춰 주기적으로 업데이트되며, 일부 항목은
            플레이어 페이지의 &ldquo;갱신&rdquo; 버튼으로 최신 데이터를 직접
            불러올 수 있습니다. 게임 내 데이터 반영 시점에 따라 실제 게임과
            일시적인 차이가 있을 수 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="면책 안내">
          <p>
            ImStarGG는 Supercell과 무관한 비공식 팬 사이트이며, Supercell로부터
            제휴·후원·승인을 받지 않았습니다. 본 사이트의 콘텐츠는 Supercell의{" "}
            <a
              href="https://www.supercell.com/fan-content-policy"
              target="_blank"
              rel="noopener noreferrer"
            >
              팬 콘텐츠 정책
            </a>
            을 따릅니다. 브롤스타즈(Brawl Stars) 및 관련 명칭·이미지의 모든
            권리는 Supercell에 있습니다.
          </p>
          <p className="text-[#5C6271]">
            This content is not affiliated with, endorsed, sponsored, or
            specifically approved by Supercell and Supercell is not responsible
            for it.
          </p>
        </LegalSection>

        <LegalSection title="문의">
          <p>
            서비스 관련 문의, 오류 제보, 데이터 정정 요청은 아래 이메일로
            보내주세요. 사이트의 &ldquo;의견 보내기&rdquo; 메뉴로도 의견을 보낼
            수 있습니다.
          </p>
          <p>
            이메일:{" "}
            <a href={`mailto:${siteConfig.email}`}>{siteConfig.email}</a>
          </p>
        </LegalSection>
      </div>
    </main>
  );
}
