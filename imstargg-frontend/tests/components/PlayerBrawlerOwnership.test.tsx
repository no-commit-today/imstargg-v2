import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerBrawlerOwnership } from "@/../app/players/[tag]/_components/PlayerBrawlerOwnership";
import type { PlayerBrawler } from "@/domain/player/types";

const item = (id: number, name: string, owned: boolean) => ({
  item: { id, name, imageUrl: null, rarity: null },
  owned,
});

const baseBrawler = (overrides: Partial<PlayerBrawler> = {}): PlayerBrawler => ({
  brawler: { id: 1, name: "Edgar", rarity: "EPIC", role: "ASSASSIN", portraitImageUrl: null },
  power: 11,
  rank: 25,
  trophies: 1234,
  highestTrophies: 1500,
  prestigeLevel: 0,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharge: null,
  buffies: null,
  ...overrides,
});

describe("PlayerBrawlerOwnership", () => {
  it("가젯 슬롯이 보유/미보유에 따라 올바른 aria-label 을 가진다", () => {
    const pb = baseBrawler({
      gadgets: [item(1, "가젯1", true), item(2, "가젯2", false), item(3, "가젯3", true)],
    });
    render(<PlayerBrawlerOwnership pb={pb} />);
    expect(screen.getByLabelText("가젯 가젯1 보유")).toBeInTheDocument();
    expect(screen.getByLabelText("가젯 가젯2 미보유")).toBeInTheDocument();
    expect(screen.getByLabelText("가젯 가젯3 보유")).toBeInTheDocument();
  });

  it("가젯 슬롯 개수가 items 개수와 일치한다", () => {
    const pb = baseBrawler({
      gadgets: [item(1, "가젯1", true), item(2, "가젯2", false), item(3, "가젯3", true)],
    });
    render(<PlayerBrawlerOwnership pb={pb} />);
    const gadgetSlots = screen.getAllByLabelText(/^가젯 /);
    expect(gadgetSlots).toHaveLength(3);
  });

  it("하이퍼차지 카탈로그가 있고 보유하면 '보유' 표기, 미보유면 '미보유' 표기", () => {
    const owned = baseBrawler({ hyperCharge: item(10, "HC", true) });
    const notOwned = baseBrawler({ hyperCharge: item(10, "HC", false) });
    const { rerender } = render(<PlayerBrawlerOwnership pb={owned} />);
    expect(screen.getByLabelText("하이퍼차지 보유")).toBeInTheDocument();
    rerender(<PlayerBrawlerOwnership pb={notOwned} />);
    expect(screen.getByLabelText("하이퍼차지 미보유")).toBeInTheDocument();
  });

  it("buffies 카탈로그가 null 이면 buffy row 자체가 렌더되지 않는다", () => {
    render(<PlayerBrawlerOwnership pb={baseBrawler({ buffies: null })} />);
    expect(screen.queryByLabelText(/버피/)).toBeNull();
  });

  it("buffies 가 있으면 3종 슬롯 렌더 (보유/미보유 aria-label 정확)", () => {
    const buffy = (owned: boolean) => ({
      item: { type: "GADGET" as const, imageUrl: null },
      owned,
    });
    const pb = baseBrawler({
      buffies: {
        gadget: buffy(true),
        starPower: buffy(false),
        hyperCharge: buffy(true),
      },
    });
    render(<PlayerBrawlerOwnership pb={pb} />);
    expect(screen.getByLabelText("버피 가젯 보유")).toBeInTheDocument();
    expect(screen.getByLabelText("버피 스타파워 미보유")).toBeInTheDocument();
    expect(screen.getByLabelText("버피 하이퍼차지 보유")).toBeInTheDocument();
  });

  it("buffies 그룹이 정확히 3개 슬롯을 렌더한다", () => {
    const buffy = (owned: boolean) => ({
      item: { type: "GADGET" as const, imageUrl: null },
      owned,
    });
    const pb = baseBrawler({
      buffies: {
        gadget: buffy(true),
        starPower: buffy(false),
        hyperCharge: buffy(true),
      },
    });
    render(<PlayerBrawlerOwnership pb={pb} />);
    const buffySlots = screen.getAllByLabelText(/^버피 /);
    expect(buffySlots).toHaveLength(3);
  });
});
