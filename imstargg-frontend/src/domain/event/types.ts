import type { BattleEventModeResultType } from "@/api/types";

export interface BattleEventMode {
  id: number;
  name: string;
  iconImageUrl: string | null;
  /** 통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null. */
  resultType: BattleEventModeResultType | null;
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
