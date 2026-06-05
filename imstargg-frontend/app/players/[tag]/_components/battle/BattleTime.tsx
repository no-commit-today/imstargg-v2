"use client";

import { useState } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { formatRelativeTime, formatAbsoluteTime } from "@/lib/relativeTime";

export function BattleTime({ iso }: { iso: string }) {
  const [open, setOpen] = useState(false);
  return (
    <TooltipProvider delayDuration={0}>
      <Tooltip open={open} onOpenChange={setOpen}>
        <TooltipTrigger asChild>
          <button
            type="button"
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
              setOpen((o) => !o);
            }}
            className="battle-row-relative-time"
            style={{
              fontSize: 12,
              color: "#5C6271",
              whiteSpace: "nowrap",
              background: "none",
              border: "none",
              padding: 0,
              cursor: "pointer",
            }}
          >
            {formatRelativeTime(iso)}
          </button>
        </TooltipTrigger>
        <TooltipContent>{formatAbsoluteTime(iso)}</TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
