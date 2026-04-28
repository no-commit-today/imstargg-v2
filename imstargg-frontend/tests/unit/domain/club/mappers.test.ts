import { describe, it, expect } from "vitest";
import { toClub, toClubMember } from "@/domain/club/mappers";
import type { ClubMemberResponse, ClubResponse } from "@/api/types";

const dtoMember = (overrides: Partial<ClubMemberResponse> = {}): ClubMemberResponse => ({
  tag: "#M1",
  name: "Alice",
  nameColor: "0xfff",
  role: "MEMBER",
  trophies: 10000,
  iconId: 28000000,
  ...overrides,
});

const dtoClub = (overrides: Partial<ClubResponse> = {}): ClubResponse => ({
  tag: "2U8CVLQQ9",
  name: "팀나인",
  description: "환영합니다",
  type: "INVITE_ONLY",
  badgeId: 8000001,
  requiredTrophies: 30000,
  trophies: 1284820,
  members: [dtoMember()],
  ...overrides,
});

describe("toClubMember", () => {
  it("passes through values when role is present", () => {
    const m = toClubMember(dtoMember({ role: "PRESIDENT" }));
    expect(m.role).toBe("PRESIDENT");
    expect(m.tag).toBe("#M1");
    expect(m.name).toBe("Alice");
  });

  it("collapses null role to UNKNOWN", () => {
    const m = toClubMember(dtoMember({ role: null }));
    expect(m.role).toBe("UNKNOWN");
  });

  it("preserves nullable nameColor", () => {
    const m = toClubMember(dtoMember({ nameColor: null }));
    expect(m.nameColor).toBeNull();
  });
});

describe("toClub", () => {
  it("normalizes tag to leading-#", () => {
    expect(toClub(dtoClub({ tag: "2U8CVLQQ9" })).tag).toBe("#2U8CVLQQ9");
    expect(toClub(dtoClub({ tag: "#2U8CVLQQ9" })).tag).toBe("#2U8CVLQQ9");
  });

  it("collapses null type to UNKNOWN", () => {
    expect(toClub(dtoClub({ type: null })).type).toBe("UNKNOWN");
  });

  it("maps every member", () => {
    const club = toClub(
      dtoClub({
        members: [
          dtoMember({ tag: "#A", role: "PRESIDENT" }),
          dtoMember({ tag: "#B", role: null }),
        ],
      }),
    );
    expect(club.members.map((m) => m.role)).toEqual(["PRESIDENT", "UNKNOWN"]);
  });
});
