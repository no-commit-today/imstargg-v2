import type { BattleEventResultType } from "@/api/types";

export interface BattleEventMode {
  id: number;
  name: string;
  iconImageUrl: string | null;
  resultType: BattleEventResultType;
}

export interface BattleEvent {
  id: number;
  mode: BattleEventMode | null;
  mapName: string;
}

export interface RotationEvent {
  event: BattleEvent;
  startTime: string;
  endTime: string;
}
