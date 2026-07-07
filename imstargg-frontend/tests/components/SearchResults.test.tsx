import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { PlayerSearchResults, ClubSearchResults } from "@/components/shared/SearchResults";
import type { PlayerResponse, ClubResponse, SliceResponse } from "@/api/types";

// PlayerResponse.name / ClubResponse.name 은 MessagesResponse 가 아니라 평문 string
// (게임 콘텐츠 엔티티인 브롤러/이벤트와 달리 매퍼가 pickName 을 거치지 않는다).
function playerDto(overrides: Partial<PlayerResponse> = {}): PlayerResponse {
  return {
    tag: "#P0",
    name: "야옹이",
    nameColor: null,
    iconId: 1,
    trophies: 50000,
    highestTrophies: 51000,
    expLevel: 100,
    expPoints: 0,
    qualifiedFromChampionshipChallenge: false,
    victories3vs3: 10,
    victoriesSolo: 1,
    victoriesDuo: 1,
    bestRoboRumbleTime: 0,
    bestTimeAsBigBrawler: 0,
    totalPrestigeLevel: 0,
    club: { tag: "#C0", name: "야옹클럽" },
    ...overrides,
  };
}

function clubDto(overrides: Partial<ClubResponse> = {}): ClubResponse {
  return {
    tag: "#C0",
    name: "야옹클럽",
    description: "",
    type: "OPEN",
    badgeId: 1,
    requiredTrophies: 0,
    trophies: 100000,
    members: [
      { tag: "#M1", name: "멤버1", nameColor: null, role: "MEMBER", trophies: 1000, iconId: 1 },
      { tag: "#M2", name: "멤버2", nameColor: null, role: "MEMBER", trophies: 2000, iconId: 1 },
    ],
    ...overrides,
  };
}

describe("PlayerSearchResults", () => {
  it("초기 결과를 렌더한다", () => {
    const initial: SliceResponse<PlayerResponse> = {
      content: [playerDto()],
      hasNext: false,
    };
    renderWithProviders(<PlayerSearchResults query="야옹이" initial={initial} />);
    expect(screen.getByText("야옹이")).toBeInTheDocument();
    expect(screen.getByText("#P0")).toBeInTheDocument();
    expect(screen.getByText("· 야옹클럽")).toBeInTheDocument();
  });

  it("더보기 클릭 시 다음 페이지를 불러와 이어붙인다", async () => {
    const initial: SliceResponse<PlayerResponse> = {
      content: [playerDto({ tag: "#P0", name: "야옹이" })],
      hasNext: true,
    };
    server.use(
      http.get("*/v1/player/search", ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("offset")).toBe("20");
        return HttpResponse.json({
          content: [playerDto({ tag: "#P1", name: "댕댕이" })],
          hasNext: false,
        });
      }),
    );

    renderWithProviders(<PlayerSearchResults query="야옹이" initial={initial} />);
    expect(screen.getByText("야옹이")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "더보기" }));

    expect(await screen.findByText("댕댕이")).toBeInTheDocument();
    expect(screen.getByText("야옹이")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "더보기" })).not.toBeInTheDocument();
  });

  it("초기 결과가 비어있으면 검색 결과 없음 문구를 보여준다", () => {
    const initial: SliceResponse<PlayerResponse> = { content: [], hasNext: false };
    renderWithProviders(<PlayerSearchResults query="존재하지않음" initial={initial} />);
    expect(
      screen.getByText('"존재하지않음" 검색 결과가 없습니다.'),
    ).toBeInTheDocument();
  });
});

describe("ClubSearchResults", () => {
  it("초기 결과를 렌더한다", () => {
    const initial: SliceResponse<ClubResponse> = {
      content: [clubDto()],
      hasNext: false,
    };
    renderWithProviders(<ClubSearchResults query="야옹클럽" initial={initial} />);
    expect(screen.getByText("야옹클럽")).toBeInTheDocument();
    expect(screen.getByText("#C0")).toBeInTheDocument();
    expect(screen.getByText("· 멤버 2명")).toBeInTheDocument();
  });

  it("더보기 클릭 시 다음 페이지를 불러와 이어붙인다", async () => {
    const initial: SliceResponse<ClubResponse> = {
      content: [clubDto({ tag: "#C0", name: "야옹클럽" })],
      hasNext: true,
    };
    server.use(
      http.get("*/v1/club/search", ({ request }) => {
        const url = new URL(request.url);
        expect(url.searchParams.get("offset")).toBe("20");
        return HttpResponse.json({
          content: [clubDto({ tag: "#C1", name: "댕댕클럽" })],
          hasNext: false,
        });
      }),
    );

    renderWithProviders(<ClubSearchResults query="야옹클럽" initial={initial} />);
    expect(screen.getByText("야옹클럽")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "더보기" }));

    expect(await screen.findByText("댕댕클럽")).toBeInTheDocument();
    expect(screen.getByText("야옹클럽")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "더보기" })).not.toBeInTheDocument();
  });

  it("초기 결과가 비어있으면 검색 결과 없음 문구를 보여준다", () => {
    const initial: SliceResponse<ClubResponse> = { content: [], hasNext: false };
    renderWithProviders(<ClubSearchResults query="존재하지않음" initial={initial} />);
    expect(
      screen.getByText('"존재하지않음" 검색 결과가 없습니다.'),
    ).toBeInTheDocument();
  });
});
