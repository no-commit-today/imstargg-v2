"use client";

import { BattleList } from "./battle/BattleList";

export function PlayerBattles({ tag }: { tag: string }) {
  return <BattleList tag={tag} />;
}
