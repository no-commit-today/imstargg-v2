/*
 * 개인정보처리방침 — 표준 항목을 포함한 초안입니다.
 * 법적 효력이 있는 문서이므로, 실제 서비스 운영 형태(수집 항목, 보관 기간,
 * 위탁 업체 등)에 맞게 운영자가 최종 검토·수정한 뒤 공개해야 합니다.
 * 특히 Google AdSense/Analytics 외 다른 제3자 서비스를 추가하면 본 문서도
 * 함께 갱신해야 합니다.
 */
import type { Metadata } from "next";
import { siteConfig } from "@/config/site";
import { buildMetadata } from "@/lib/seo";
import { LegalSection } from "../_components/LegalSection";

export const metadata: Metadata = buildMetadata({
  title: "개인정보처리방침 | ImStarGG",
  description:
    "ImStarGG의 개인정보 수집·이용, 쿠키 및 Google 광고·분석 사용에 관한 안내입니다.",
  path: "/privacy",
});

const LAST_UPDATED = "2026년 6월 17일";

export default function PrivacyPage() {
  return (
    <main className="mx-auto w-full max-w-7xl px-4 py-6 space-y-6">
      <header className="space-y-1">
        <h1 className="font-display text-3xl tracking-[0.02em]">
          개인정보처리방침
        </h1>
        <p className="text-sm text-[#5C6271]">최종 업데이트: {LAST_UPDATED}</p>
      </header>

      <div className="max-w-3xl space-y-8">
        <LegalSection title="개요">
          <p>
            ImStarGG(이하 &ldquo;사이트&rdquo;)는 이용자의 개인정보를 중요하게
            생각하며, 본 개인정보처리방침을 통해 사이트가 어떤 정보를 수집하고
            어떻게 이용하는지 안내합니다. 사이트는 브롤스타즈 통계 조회 서비스를
            제공하며, 회원 가입 없이 누구나 이용할 수 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="수집하는 정보와 목적">
          <p>
            사이트는 회원 가입을 요구하지 않으며, 이름·연락처 등 직접적인 개인
            식별 정보를 별도로 수집하지 않습니다. 다만 서비스 제공과 개선을 위해
            다음 정보가 자동으로 수집될 수 있습니다.
          </p>
          <ul>
            <li>
              <strong>이용 기록 및 기기 정보</strong> — 접속 일시, 브라우저 종류,
              운영체제, 기기 유형, 참조 페이지 등(웹 분석 목적).
            </li>
            <li>
              <strong>검색어 및 조회 기록</strong> — 입력한 플레이어·클럽 태그 등
              검색 정보(서비스 제공 목적). 일부 최근 검색 기록은 이용자 편의를
              위해 이용자 브라우저(로컬 저장소)에만 저장되며 사이트 서버로
              전송되지 않습니다.
            </li>
            <li>
              <strong>쿠키 및 유사 기술</strong> — 아래 &ldquo;쿠키 및 유사
              기술&rdquo; 항목을 참고하세요.
            </li>
          </ul>
          <p>
            수집된 정보는 (1) 통계 서비스 제공, (2) 서비스 이용 현황 분석 및
            개선, (3) 광고 게재의 목적으로만 이용됩니다.
          </p>
        </LegalSection>

        <LegalSection title="쿠키 및 유사 기술">
          <p>
            사이트는 이용자 경험 개선, 이용 통계 분석, 광고 게재를 위해
            쿠키(cookie) 및 유사 기술을 사용합니다. 쿠키는 이용자의 브라우저에
            저장되는 작은 텍스트 파일로, 이용자는 언제든지 브라우저 설정을 통해
            쿠키 저장을 거부하거나 삭제할 수 있습니다. 다만 쿠키 사용을 거부할
            경우 일부 기능 이용에 제한이 있을 수 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="제3자 광고 및 Google AdSense">
          <p>
            사이트는 Google이 제공하는 광고 서비스인 <strong>Google AdSense</strong>
            를 사용하여 광고를 게재합니다.
          </p>
          <ul>
            <li>
              Google을 포함한 제3자 광고 사업자는 쿠키 및 광고 식별자를 사용하여
              이용자가 본 사이트 및 다른 웹사이트를 방문한 기록을 바탕으로 맞춤형
              광고를 제공할 수 있습니다.
            </li>
            <li>
              이용자는{" "}
              <a
                href="https://www.google.com/settings/ads"
                target="_blank"
                rel="noopener noreferrer"
              >
                Google 광고 설정
              </a>
              에서 맞춤 광고를 비활성화할 수 있으며,{" "}
              <a
                href="https://www.aboutads.info/choices"
                target="_blank"
                rel="noopener noreferrer"
              >
                www.aboutads.info
              </a>
              에서 제3자 업체의 맞춤 광고 쿠키 사용을 일괄 거부할 수 있습니다.
            </li>
            <li>
              Google이 광고 쿠키를 사용하는 방식에 대한 자세한 내용은{" "}
              <a
                href="https://policies.google.com/technologies/ads"
                target="_blank"
                rel="noopener noreferrer"
              >
                Google의 광고 관련 개인정보처리방침
              </a>
              을 참고하시기 바랍니다.
            </li>
          </ul>
        </LegalSection>

        <LegalSection title="웹 분석 도구 (Google Analytics)">
          <p>
            사이트는 이용 현황 분석을 위해 <strong>Google Analytics</strong>를
            사용합니다. Google Analytics는 쿠키를 통해 이용자의 사이트 이용 정보를
            수집하며, 이 정보는 사이트 이용 통계 분석에 사용됩니다. 이용자는{" "}
            <a
              href="https://tools.google.com/dlpage/gaoptout"
              target="_blank"
              rel="noopener noreferrer"
            >
              Google Analytics 차단 브라우저 부가기능
            </a>
            을 설치하여 데이터 수집을 거부할 수 있습니다.
          </p>
        </LegalSection>

        <LegalSection title="정보의 보관 및 제3자 제공">
          <p>
            사이트는 수집한 정보를 서비스 제공에 필요한 기간 동안만 보관하며,
            관련 법령이 정한 경우를 제외하고 이용자의 동의 없이 제3자에게
            판매·대여·제공하지 않습니다. 다만 위에 명시한 Google 등 광고·분석
            서비스 제공사는 각 사의 정책에 따라 정보를 처리합니다.
          </p>
        </LegalSection>

        <LegalSection title="이용자의 권리">
          <p>
            이용자는 자신의 정보에 대한 열람·정정·삭제를 요청할 수 있으며,
            브라우저 설정을 통해 쿠키 수집을 거부할 권리가 있습니다. 관련 요청은
            아래 연락처로 문의해 주시기 바랍니다.
          </p>
        </LegalSection>

        <LegalSection title="아동의 개인정보">
          <p>
            사이트는 만 14세 미만 아동을 대상으로 하지 않으며, 아동의 개인정보를
            고의로 수집하지 않습니다.
          </p>
        </LegalSection>

        <LegalSection title="개인정보처리방침의 변경">
          <p>
            본 방침은 법령 또는 서비스 변경에 따라 수정될 수 있으며, 변경 시 본
            페이지를 통해 공지합니다.
          </p>
        </LegalSection>

        <LegalSection title="문의">
          <p>
            개인정보 관련 문의:{" "}
            <a href={`mailto:${siteConfig.email}`}>{siteConfig.email}</a>
          </p>
        </LegalSection>
      </div>
    </main>
  );
}
