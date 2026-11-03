import { describe, it, expect } from "vitest";
import { eventMetadataDescription } from "@/domain/event/metadata";
import type { BattleEvent, BattleEventMode } from "@/domain/event/types";

const event = (mode: Pick<BattleEventMode, "name" | "resultType">): BattleEvent => ({
  id: 15000001,
  mode: { id: 0, iconImageUrl: null, ...mode },
  mapName: "광산 분쟁",
});

describe("eventMetadataDescription", () => {
  it("통계 미지원 모드면 결과 타입을 쓰지 않는다", () => {
    expect(eventMetadataDescription(event({ name: "와이프아웃", resultType: null }))).toBe(
      "모드: 와이프아웃",
    );
  });

  it("순위형이면 결과 타입: 순위형", () => {
    expect(eventMetadataDescription(event({ name: "솔로 쇼다운", resultType: "RANK" }))).toBe(
      "모드: 솔로 쇼다운 · 결과 타입: 순위형",
    );
  });

  it("결과형이면 결과 타입: 결과형", () => {
    expect(eventMetadataDescription(event({ name: "젬 그랩", resultType: "RESULT" }))).toBe(
      "모드: 젬 그랩 · 결과 타입: 결과형",
    );
  });

  it("모드가 없으면 이벤트로 쓴다", () => {
    expect(
      eventMetadataDescription({ ...event({ name: "x", resultType: "RESULT" }), mode: null }),
    ).toBe("모드: 이벤트");
  });
});
