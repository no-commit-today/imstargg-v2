"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { COUNTRY_CODES, COUNTRY_META, type Country } from "@/domain/ranking/country";

type Props = {
  current: Country;
};

export function CountrySelect({ current }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  function handleChange(code: string) {
    const params = new URLSearchParams(searchParams.toString());
    if (code === "global") {
      params.delete("country");
    } else {
      params.set("country", code);
    }
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }

  return (
    <Select value={current} onValueChange={handleChange}>
      <SelectTrigger aria-label="국가 선택" className="w-[140px]">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {COUNTRY_CODES.map((code) => (
          <SelectItem key={code} value={code}>
            <span aria-hidden="true">{COUNTRY_META[code].flag}</span>
            <span>{COUNTRY_META[code].label}</span>
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
