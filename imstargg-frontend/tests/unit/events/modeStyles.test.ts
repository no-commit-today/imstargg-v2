import { describe, it, expect } from "vitest";
import { getModeStyle } from "@app/events/_components/modeStyles";

describe("getModeStyle", () => {
  it("색이 없으면(null/undefined) 회색 기본 스타일을 반환한다", () => {
    expect(getModeStyle(null)).toEqual({ bg: "#2A2E38", fg: "#FFFFFF" });
    expect(getModeStyle(undefined)).toEqual({ bg: "#2A2E38", fg: "#FFFFFF" });
  });

  it("밝은 배경색이면 어두운 텍스트색(#14161B)을 고른다", () => {
    // BOUNTY 옐로우 — 상대 명도 높음
    expect(getModeStyle("#FFC60A")).toEqual({ bg: "#FFC60A", fg: "#14161B" });
  });

  it("어두운 배경색이면 흰색 텍스트색(#FFFFFF)을 고른다", () => {
    // GEM GRAB 퍼플 — 상대 명도 낮음
    expect(getModeStyle("#8F43EA")).toEqual({ bg: "#8F43EA", fg: "#FFFFFF" });
  });
});
