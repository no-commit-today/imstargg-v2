import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("플레이어 페이지가 정상 데이터로 렌더된다", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await expect(
    page.getByText(TEST_TAGS.player, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/트로피/).first()).toBeVisible();
});

test.skip("갱신 버튼 클릭 시 토스트 → 폴링 → 완료 토스트 (CORS 배포 후 활성화)", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  const renewBtn = page.getByRole("button", { name: "플레이어 정보 갱신" });
  await renewBtn.waitFor({ state: "visible", timeout: 15_000 });
  await expect(renewBtn).toBeEnabled({ timeout: 15_000 });
  await renewBtn.click();

  await expect(page.getByText(/갱신 요청됨/)).toBeVisible({ timeout: 5_000 });
  await expect(page.getByText(/갱신 완료/)).toBeVisible({ timeout: 60_000 });
});
