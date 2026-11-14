import type { BattleEvent } from "@/domain/event/types";

const RESULT_TYPE_LABEL = { RESULT: "결과형", RANK: "순위형" } as const;

/** 이벤트 상세 메타 설명. 통계를 모으지 않는 모드(resultType null)는 결과 타입을 쓰지 않는다. */
export function eventMetadataDescription(event: BattleEvent): string {
  const modeName = event.mode?.name ?? "이벤트";
  const resultType = event.mode?.resultType;
  if (!resultType) return `모드: ${modeName}`;
  return `모드: ${modeName} · 결과 타입: ${RESULT_TYPE_LABEL[resultType]}`;
}
