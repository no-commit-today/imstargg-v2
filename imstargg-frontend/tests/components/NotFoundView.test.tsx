import { describe, it, expect, beforeEach, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../setup/msw";
import { renderWithProviders } from "../setup/renderWithProviders";
import { screen } from "@testing-library/react";
import { NotFoundView } from "@/components/shared/NotFoundView";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ refresh: vi.fn() }),
}));

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

describe("NotFoundView (player)", () => {
  beforeEach(() => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "COMPLETED",
          progressing: false,
          canRenew: true,
          requestedAt: null,
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        }),
      ),
    );
  });

  it("찾을 수 없음 안내 + 갱신 버튼 + 홈으로 링크를 노출한다", async () => {
    renderWithProviders(<NotFoundView kind="player" tag={TAG} />);

    expect(
      await screen.findByRole("heading", { name: "플레이어를 찾을 수 없어요" }),
    ).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /갱신/ })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "홈으로" })).toBeInTheDocument();
  });
});
