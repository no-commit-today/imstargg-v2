import { describe, it, expect } from "vitest";
import { routes } from "@/config/routes";

describe("routes.player / routes.club", () => {
  it("태그를 canonical %23 인코딩 경로로 만든다", () => {
    expect(routes.player("#88J8Q9RPV")).toBe("/players/%2388J8Q9RPV");
    expect(routes.club("#2U8CVLQQ9")).toBe("/clubs/%232U8CVLQQ9");
  });

  it("# 없는 태그도 정규화해 동일한 경로를 만든다", () => {
    expect(routes.player("88J8Q9RPV")).toBe("/players/%2388J8Q9RPV");
    expect(routes.club("2u8cvlqq9")).toBe("/clubs/%232U8CVLQQ9");
  });
});
