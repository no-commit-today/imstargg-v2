import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../setup/msw";
import { renderWithProviders } from "../../setup/renderWithProviders";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RenewalButton } from "@/components/shared/RenewalButton";

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

describe("RenewalButton (player) — 폴링 + 완료", () => {
  beforeEach(() => {
    let progressing = false;
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: progressing ? "EXECUTING" : "COMPLETED",
          progressing,
          canRenew: !progressing,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: progressing ? null : "2026-04-27T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-27T00:00:01Z",
        }),
      ),
      http.post(`*/v1/players/${ENC}/renewal`, () => {
        progressing = true;
        setTimeout(() => {
          progressing = false;
        }, 1500);
        return HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-27T00:00:02Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        });
      }),
    );
  });

  it("갱신 클릭 → '갱신 요청됨' → 폴링 후 '갱신 완료' 토스트", async () => {
    renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    expect(await screen.findByText(/갱신 요청됨/)).toBeInTheDocument();

    expect(
      await screen.findByText(/갱신 완료/, {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  });
});

describe("RenewalButton (player) — 첫 갱신 (renewal record 없음)", () => {
  beforeEach(() => {
    let progressing = false;
    let recordExists = false;
    server.use(
      // 첫 GET 은 404 — 백엔드는 renewal row 가 없으면 NOT_FOUND 응답
      http.get(`*/v1/players/${ENC}/renewal`, () => {
        if (!recordExists) return new HttpResponse(null, { status: 404 });
        return HttpResponse.json({
          tag: TAG,
          status: progressing ? "EXECUTING" : "COMPLETED",
          progressing,
          canRenew: !progressing,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: null,
          completedAt: progressing ? null : "2026-04-27T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-27T00:00:01Z",
        });
      }),
      http.post(`*/v1/players/${ENC}/renewal`, () => {
        recordExists = true;
        progressing = true;
        setTimeout(() => {
          progressing = false;
        }, 1500);
        return HttpResponse.json({
          tag: TAG,
          status: "EXECUTING",
          progressing: true,
          canRenew: false,
          requestedAt: "2026-04-27T00:00:02Z",
          executedAt: null,
          completedAt: null,
          renewedAt: null,
        });
      }),
    );
  });

  it("기록이 없어도 버튼이 활성화되고 첫 갱신을 트리거할 수 있다", async () => {
    renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    expect(await screen.findByText(/갱신 요청됨/)).toBeInTheDocument();
    expect(
      await screen.findByText(/갱신 완료/, {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  });
});
