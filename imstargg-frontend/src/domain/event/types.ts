import type { BattleEventModeResultType } from "@/api/types";

export interface BattleEventMode {
  id: number;
  name: string;
  iconImageUrl: string | null;
  resultType: BattleEventModeResultType;
  /** 이벤트 카드 배경에 쓰이는 모드 대표색 (#RRGGBB). 백엔드 제공. */
  color: string;
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
