import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("플레이어 페이지가 정상 데이터로 렌더된다", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await expect(
    page.getByText(TEST_TAGS.player, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/트로피/).first()).toBeVisible();
});

test("갱신 버튼 클릭 시 진행 캡션 → 폴링 → 최근 업데이트 갱신", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  const renewBtn = page.getByRole("button", { name: "플레이어 정보 갱신" });
  await renewBtn.waitFor({ state: "visible", timeout: 15_000 });
  // 이전 실행이 남긴 갱신이 진행 중이면 완료(canRenew)까지 대기
  await expect(renewBtn).toBeEnabled({ timeout: 60_000 });
  await renewBtn.click();

  // 토스트는 제거됨 — RenewalStatus 캡션이 진행/완료 상태를 노출한다
  await expect(page.getByText("최근 업데이트: 진행 중…")).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.getByText(/최근 업데이트: (방금 전|\d+분 전)/)).toBeVisible({
    timeout: 60_000,
  });
});
