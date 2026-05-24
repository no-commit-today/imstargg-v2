"use client";

import { usePathname, useSearchParams } from "next/navigation";
import Link from "next/link";
import { COUNTRY_CODES, COUNTRY_META, type Country } from "@/domain/ranking/country";

type Props = {
  current: Country;
};

export function CountryFlagPills({ current }: Props) {
  const pathname = usePathname();
  const searchParams = useSearchParams();

  function hrefFor(code: Country): string {
    const params = new URLSearchParams(searchParams.toString());
    if (code === "global") {
      params.delete("country");
    } else {
      params.set("country", code);
    }
    const qs = params.toString();
    return qs ? `${pathname}?${qs}` : pathname;
  }

  return (
    <nav aria-label="국가 선택" className="country-pills">
      {COUNTRY_CODES.map((code) => {
        const meta = COUNTRY_META[code];
        const active = code === current;
        return (
          <Link
            key={code}
            href={hrefFor(code)}
            scroll={false}
            replace
            className="country-pill"
            data-active={active ? "true" : "false"}
            aria-current={active ? "page" : undefined}
          >
            <span className="country-pill-flag" aria-hidden="true">
              {meta.flag}
            </span>
            <span className="country-pill-label">{meta.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}
