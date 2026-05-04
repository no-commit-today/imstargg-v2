import { describe, it, expect } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../../setup/msw";
import { renderWithProviders } from "../../../setup/renderWithProviders";
import { screen } from "@testing-library/react";
import { RenewalStatus } from "@/components/shared/RenewalStatus";

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

describe("RenewalStatus (player)", () => {
  it("renewedAt 이 있으면 '최근 업데이트: ...' 텍스트를 보여준다", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "COMPLETED",
          progressing: false,
          canRenew: true,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: "2026-04-27T00:00:01Z",
          renewedAt: "2026-04-27T00:00:01Z",
        }),
      ),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/최근 업데이트:/)).toBeInTheDocument();
  });

  it("renewedAt 이 null 이면 '최근 업데이트 없음' 표시", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () => new HttpResponse(null, { status: 404 })),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/최근 업데이트 없음/)).toBeInTheDocument();
  });

  it("progressing 일 때 '진행 중…' 표시", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        }),
      ),
    );
    renderWithProviders(<RenewalStatus kind="player" tag={TAG} />);
    expect(await screen.findByText(/진행 중…/)).toBeInTheDocument();
  });
});
