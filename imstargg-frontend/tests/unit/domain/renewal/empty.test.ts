import { describe, it, expect } from "vitest";
import { emptyRenewal } from "@/domain/renewal/empty";

describe("emptyRenewal", () => {
  it("normalizes the tag", () => {
    expect(emptyRenewal("88J8Q9RPV").tag).toBe("#88J8Q9RPV");
    expect(emptyRenewal("%2388J8Q9RPV").tag).toBe("#88J8Q9RPV");
  });

  it("permits a first-time renewal", () => {
    const r = emptyRenewal("#TAG");
    expect(r.canRenew).toBe(true);
    expect(r.progressing).toBe(false);
    expect(r.status).toBe("NOT_FOUND");
  });
});
