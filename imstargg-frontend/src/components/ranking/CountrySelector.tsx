"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  COUNTRY_CODES,
  COUNTRY_LABELS,
  type Country,
} from "@/domain/ranking/country";

type Props = {
  current: Country;
};

export function CountrySelector({ current }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const onChange = (next: string) => {
    const params = new URLSearchParams(searchParams.toString());
    if (next === "global") {
      params.delete("country");
    } else {
      params.set("country", next);
    }
    const qs = params.toString();
    router.push(qs ? `${pathname}?${qs}` : pathname);
  };

  return (
    <Select value={current} onValueChange={onChange}>
      <SelectTrigger className="w-32" aria-label="국가 선택">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {COUNTRY_CODES.map((code) => (
          <SelectItem key={code} value={code}>
            {COUNTRY_LABELS[code]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
