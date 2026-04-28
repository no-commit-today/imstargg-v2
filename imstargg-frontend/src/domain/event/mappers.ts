import type {
  BattleEventModeResponse,
  BattleEventResponse,
  RotationBattleEventResponse,
} from "@/api/types";
import type {
  BattleEvent,
  BattleEventMode,
  RotationEvent,
} from "@/domain/event/types";
import { pickName } from "@/domain/i18n";

export function toBattleEventMode(dto: BattleEventModeResponse): BattleEventMode {
  return {
    id: dto.id,
    name: pickName(dto.name),
    iconImageUrl: dto.iconImageUrl,
    resultType: dto.resultType,
  };
}

export function toBattleEvent(dto: BattleEventResponse): BattleEvent {
  return {
    id: dto.id,
    mode: dto.mode ? toBattleEventMode(dto.mode) : null,
    mapName: pickName(dto.mapName),
  };
}

export function toRotationEvent(dto: RotationBattleEventResponse): RotationEvent {
  return {
    event: toBattleEvent(dto.event),
    startTime: dto.startTime,
    endTime: dto.endTime,
  };
}
