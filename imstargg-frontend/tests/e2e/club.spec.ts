import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("클럽 페이지가 정상 데이터로 렌더된다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  await expect(
    page.getByText(TEST_TAGS.club, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/멤버/).first()).toBeVisible();
});

test("멤버 행 클릭 시 플레이어 페이지로 이동한다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  const firstMemberRow = page.getByRole("row").nth(1); // [0] is header
  await firstMemberRow.waitFor({ state: "visible", timeout: 15_000 });
  await firstMemberRow.click();
  await page.waitForURL(/\/players\//, { timeout: 15_000 });
  expect(page.url()).toContain("/players/");
});

test("SEO 메타데이터: title에 클럽 태그가 포함되고 canonical 태그가 존재한다", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  await expect(page).toHaveTitle(new RegExp(TEST_TAGS.club.replace("#", "#?")), {
    timeout: 15_000,
  });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});

test.skip("갱신 버튼 클릭 시 토스트 → 폴링 → 완료 토스트 (CORS 배포 후 활성화)", async ({ page }) => {
  await page.goto(`/clubs/${TEST_TAGS.clubEncoded}`);
  const renewBtn = page.getByRole("button", { name: "클럽 정보 갱신" });
  await renewBtn.waitFor({ state: "visible", timeout: 15_000 });
  await expect(renewBtn).toBeEnabled({ timeout: 15_000 });
  await renewBtn.click();
  await expect(page.getByText(/갱신 요청됨/)).toBeVisible({ timeout: 5_000 });
  await expect(page.getByText(/갱신 완료/)).toBeVisible({ timeout: 60_000 });
});
