import { describe, it, expect } from "vitest";
import { getModeStyle } from "@app/events/_components/modeStyles";

describe("getModeStyle", () => {
  it("젬그랩(modeId 0)은 보라 배경이다", () => {
    // modeId 0 은 falsy — 존재 검사를 truthiness 로 하면 회색으로 떨어진다
    expect(getModeStyle(0)).toEqual({ bg: "#8F43EA", fg: "#FFFFFF" });
  });

  it("바운티(3)처럼 밝은 배경이면 어두운 글자색이다", () => {
    expect(getModeStyle(3)).toEqual({ bg: "#FFC60A", fg: "#14161B" });
  });

  it("목록에 없는 모드(48 아레나, 25)와 null/undefined 는 기본 회색이다", () => {
    for (const id of [48, 25, null, undefined]) {
      expect(getModeStyle(id)).toEqual({ bg: "#2A2E38", fg: "#FFFFFF" });
    }
  });

  it("쇼다운 3종(6, 9, 38)은 같은 초록 배경이다", () => {
    for (const id of [6, 9, 38]) {
      expect(getModeStyle(id).bg).toBe("#95D449");
    }
  });
});
