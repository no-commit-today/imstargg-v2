import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { PlayerBrawlerOwnership } from "@/../app/players/[tag]/_components/PlayerBrawlerOwnership";
import type { PlayerBrawler } from "@/domain/player/types";

const item = (id: number, owned: boolean) => ({
  item: { id, name: `i${id}`, imageUrl: null, rarity: null },
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
  it("가젯 보유 갯수와 전체 갯수를 ratio 로 표시한다", () => {
    const pb = baseBrawler({ gadgets: [item(1, true), item(2, false), item(3, true)] });
    render(<PlayerBrawlerOwnership pb={pb} />);
    expect(screen.getByLabelText("가젯 2/3")).toBeInTheDocument();
  });

  it("하이퍼차지 카탈로그가 있고 보유하면 '보유' 표기, 미보유면 '미보유' 표기", () => {
    const owned = baseBrawler({ hyperCharge: item(10, true) });
    const notOwned = baseBrawler({ hyperCharge: item(10, false) });
    const { rerender } = render(<PlayerBrawlerOwnership pb={owned} />);
    expect(screen.getByLabelText("하이퍼차지 보유")).toBeInTheDocument();
    rerender(<PlayerBrawlerOwnership pb={notOwned} />);
    expect(screen.getByLabelText("하이퍼차지 미보유")).toBeInTheDocument();
  });

  it("buffies 카탈로그가 null 이면 buffy row 자체가 렌더되지 않는다", () => {
    render(<PlayerBrawlerOwnership pb={baseBrawler({ buffies: null })} />);
    expect(screen.queryByLabelText(/버피/)).toBeNull();
  });

  it("buffies 가 있으면 3종 표시 (보유는 채움, 미보유는 흐림)", () => {
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
});
