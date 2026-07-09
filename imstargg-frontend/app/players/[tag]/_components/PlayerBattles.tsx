"use client";

import type { BattleResponse, SliceResponse } from "@/api/types";
import { BattleList } from "./battle/BattleList";

type Props = {
  tag: string;
  initialBattles?: SliceResponse<BattleResponse>;
  battlesFetchedAt?: number;
};

export function PlayerBattles({ tag, initialBattles, battlesFetchedAt }: Props) {
  return (
    <BattleList
      tag={tag}
      initialBattles={initialBattles}
      battlesFetchedAt={battlesFetchedAt}
    />
  );
}
