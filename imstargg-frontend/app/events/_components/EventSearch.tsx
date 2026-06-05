"use client";

import { useEffect, useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { useEventSearch } from "@/hooks/queries/useEventSearch";
import { EventSearchResults } from "./EventSearchResults";

interface Props {
  children: React.ReactNode;  // default content shown when search is empty
}

export function EventSearch({ children }: Props) {
  const [text, setText] = useState("");
  const [debounced, setDebounced] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setDebounced(text), 300);
    return () => clearTimeout(t);
  }, [text]);

  const trimmed = debounced.trim();
  const { data, isPending } = useEventSearch(debounced);
  const active = trimmed.length > 0;

  return (
    <div className="space-y-4">
      <div className="relative w-full sm:max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="맵 이름 검색"
          className="pl-9"
          aria-label="맵 이름 검색"
        />
      </div>
      {active ? <EventSearchResults results={data} loading={isPending} /> : children}
    </div>
  );
}
