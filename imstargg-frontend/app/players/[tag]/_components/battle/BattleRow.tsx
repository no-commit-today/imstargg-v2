"use client";

import { useState, type KeyboardEvent } from "react";
import { BattleRowHeader } from "./BattleRowHeader";
import { BattleExpandedTeams } from "./BattleExpandedTeams";
import type { Battle } from "@/domain/battle/types";

interface Props {
  battle: Battle;
  ownerTag: string;
}

export function BattleRow({ battle, ownerTag }: Props) {
  const [expanded, setExpanded] = useState(false);
  const toggle = () => setExpanded((v) => !v);
  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      toggle();
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      aria-expanded={expanded}
      onClick={toggle}
      onKeyDown={onKey}
      className="card"
      style={{ overflow: "hidden", cursor: "pointer" }}
    >
      <BattleRowHeader battle={battle} ownerTag={ownerTag} expanded={expanded} />
      {expanded && (
        <div onClick={(e) => e.stopPropagation()} style={{ borderTop: "1px solid #ECEEF3" }}>
          <BattleExpandedTeams battle={battle} ownerTag={ownerTag} />
        </div>
      )}
    </div>
  );
}
