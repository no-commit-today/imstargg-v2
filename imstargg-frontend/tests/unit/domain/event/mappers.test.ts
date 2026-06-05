import { describe, it, expect } from "vitest";
import {
  toBattleEvent,
  toBattleEventMode,
  toRotationEvent,
} from "@/domain/event/mappers";
import type {
  BattleEventModeResponse,
  BattleEventResponse,
  RotationBattleEventResponse,
} from "@/api/types";

const dtoMode = (overrides: Partial<BattleEventModeResponse> = {}): BattleEventModeResponse => ({
  id: 14000000,
  name: { ko: "젬 그랩", en: "Gem Grab" },
  iconImageUrl: "https://cdn/gem.png",
  resultType: "RESULT",
  color: "#8F43EA",
  ...overrides,
});

const dtoEvent = (overrides: Partial<BattleEventResponse> = {}): BattleEventResponse => ({
  id: 15000001,
  mode: dtoMode(),
  mapName: { ko: "광산 분쟁", en: "Hard Rock Mine" },
  ...overrides,
});

describe("toBattleEventMode", () => {
  it("flattens MessagesResponse name to ko-first", () => {
    const result = toBattleEventMode(dtoMode());
    expect(result).toEqual({
      id: 14000000,
      name: "젬 그랩",
      iconImageUrl: "https://cdn/gem.png",
      resultType: "RESULT",
      color: "#8F43EA",
    });
  });

  it("preserves null iconImageUrl", () => {
    const result = toBattleEventMode(dtoMode({ iconImageUrl: null }));
    expect(result.iconImageUrl).toBeNull();
  });

  it("falls back to en when ko missing", () => {
    const result = toBattleEventMode(dtoMode({ name: { en: "Gem Grab" } }));
    expect(result.name).toBe("Gem Grab");
  });
});

describe("toBattleEvent", () => {
  it("flattens mapName and maps mode", () => {
    const result = toBattleEvent(dtoEvent());
    expect(result.id).toBe(15000001);
    expect(result.mapName).toBe("광산 분쟁");
    expect(result.mode?.name).toBe("젬 그랩");
  });

  it("preserves null mode", () => {
    const result = toBattleEvent(dtoEvent({ mode: null }));
    expect(result.mode).toBeNull();
  });

  it("returns (unknown) when mapName messages object is empty", () => {
    const result = toBattleEvent(dtoEvent({ mapName: {} }));
    expect(result.mapName).toBe("(unknown)");
  });
});

describe("toRotationEvent", () => {
  it("preserves ISO startTime/endTime and maps inner event", () => {
    const dto: RotationBattleEventResponse = {
      event: dtoEvent(),
      startTime: "2026-04-28T00:00:00Z",
      endTime: "2026-04-29T00:00:00Z",
    };
    const result = toRotationEvent(dto);
    expect(result.startTime).toBe("2026-04-28T00:00:00Z");
    expect(result.endTime).toBe("2026-04-29T00:00:00Z");
    expect(result.event.mapName).toBe("광산 분쟁");
  });
});
