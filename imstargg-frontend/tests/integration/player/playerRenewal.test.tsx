import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../setup/msw";
import { renderWithProviders } from "../../setup/renderWithProviders";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { RenewalStatus } from "@/components/shared/RenewalStatus";

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

/** PlayerHero 의 실제 배치 — 상태 캡션과 버튼이 각각 useRenewal 을 호출한다. */
function PlayerHeroRenewal() {
  return (
    <>
      <RenewalStatus kind="player" tag={TAG} />
      <RenewalButton kind="player" tag={TAG} />
    </>
  );
}

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

  it("갱신 클릭 → 폴링 동안 비활성화되었다가 다시 활성화된다", async () => {
    renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    await waitFor(() => expect(button).toBeDisabled());
    await waitFor(() => expect(button).not.toBeDisabled(), { timeout: 5000 });
  });

});

describe("RenewalStatus (player) — 실패 피드백", () => {
  it("갱신 요청이 실패하면 버튼 옆 캡션에 실패가 표시된다", async () => {
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
      http.post(`*/v1/players/${ENC}/renewal`, () => new HttpResponse(null, { status: 500 })),
    );
    renderWithProviders(<PlayerHeroRenewal />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    await userEvent.click(button);

    // 버튼과 캡션은 서로 다른 useRenewal 인스턴스 — mutation 상태가 공유되어야 캡션에 뜬다
    expect(await screen.findByText(/갱신 실패/, undefined, { timeout: 5000 })).toBeInTheDocument();
  });

  it("갱신이 FAILED 로 끝났으면 실패가 표시되고 다시 시도할 수 있다", async () => {
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: "FAILED",
          progressing: false,
          canRenew: true,
          requestedAt: "2026-04-27T00:00:00Z",
          executedAt: "2026-04-27T00:00:01Z",
          completedAt: null,
          renewedAt: null,
        }),
      ),
    );
    renderWithProviders(<PlayerHeroRenewal />);

    expect(await screen.findByText(/갱신 실패/)).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole("button", { name: /갱신/ })).not.toBeDisabled(),
    );
  });
});

describe("RenewalButton (player) — 멈춘 갱신 복구 (progressing + canRenew)", () => {
  beforeEach(() => {
    // 오래 전 요청이 PENDING 에 갇힌 상태 — 백엔드는 stale 로 판정해 canRenew=true 를 준다
    let stuck = true;
    let progressing = true;
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG,
          status: progressing ? (stuck ? "PENDING" : "EXECUTING") : "COMPLETED",
          progressing,
          canRenew: stuck || !progressing,
          requestedAt: "2026-04-20T00:00:00Z",
          executedAt: null,
          completedAt: progressing ? null : "2026-04-27T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-27T00:00:01Z",
        }),
      ),
      http.post(`*/v1/players/${ENC}/renewal`, () => {
        stuck = false;
        progressing = true;
        setTimeout(() => {
          progressing = false;
        }, 1500);
        return HttpResponse.json({
          tag: TAG,
          status: "PENDING",
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

  it("갇힌 갱신도 버튼이 활성화되어 재시도로 복구할 수 있다", async () => {
    renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

    // progressing=true 여도 canRenew=true 면 활성화 + "갱신" 라벨
    const button = await screen.findByRole("button", { name: /갱신/ });
    await waitFor(() => expect(button).not.toBeDisabled());
    expect(button).toHaveTextContent("갱신");
    expect(button).not.toHaveTextContent("갱신 중…");

    // 재시도 → 정상 갱신 사이클로 복구
    await userEvent.click(button);
    await waitFor(() => expect(button).toBeDisabled());
    await waitFor(() => expect(button).not.toBeDisabled(), { timeout: 5000 });
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

    await waitFor(() => expect(button).toBeDisabled());
    await waitFor(() => expect(button).not.toBeDisabled(), { timeout: 5000 });
  });
});
