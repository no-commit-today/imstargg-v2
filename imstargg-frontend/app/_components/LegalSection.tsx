import type { ReactNode } from "react";

// 소개/이용약관/개인정보처리방침 등 산문형 신뢰 페이지의 공통 섹션 래퍼.
// 제목은 .section-title(좌측 옐로우 바), 본문은 14px 들여쓰기로 제목과 정렬.
// 본문 내부의 a/strong/ul 은 arbitrary variant 로 일괄 타이포 처리하므로,
// 각 페이지는 시맨틱 마크업(p, ul>li, a, strong)만 작성하면 된다.
const bodyClassName =
  "ml-[14px] space-y-3 text-sm leading-7 text-[#3B404D] sm:text-base " +
  "[&_a]:text-[#2E7CF6] [&_a]:underline [&_a]:underline-offset-2 [&_a:hover]:opacity-80 " +
  "[&_strong]:font-semibold [&_strong]:text-[#14161B] " +
  "[&_ul]:list-disc [&_ul]:space-y-1 [&_ul]:pl-5";

export function LegalSection({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="space-y-3">
      <h2 className="section-title">{title}</h2>
      <div className={bodyClassName}>{children}</div>
    </section>
  );
}
