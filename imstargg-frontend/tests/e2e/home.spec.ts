import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("홈 페이지가 로드되고 메타데이터가 노출된다", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle(/ImStarGG/);
  const description = await page
    .locator('meta[name="description"]')
    .getAttribute("content");
  expect(description ?? "").toContain("브롤스타즈");
});

test("플레이어 태그 검색 후 검색 결과를 거쳐 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/");
  const input = page.getByLabel("플레이어 이름 또는 태그 입력");
  await input.fill(TEST_TAGS.player);
  await page.getByRole("button", { name: "플레이어 검색" }).click();

  // 검색 결과 페이지로 이동 (q 쿼리에 인코딩된 태그 포함)
  await expect(page).toHaveURL(new RegExp(TEST_TAGS.playerEncoded));
  const resultRow = page.getByRole("link", { name: new RegExp(TEST_TAGS.player) });
  await resultRow.waitFor({ state: "visible", timeout: 15_000 });
  await resultRow.click();

  // 결과 행 클릭 시 상세 페이지 도착
  const bareTag = TEST_TAGS.player.replace(/^#/, "");
  await page.waitForURL(new RegExp(`/players/${bareTag}`), { timeout: 15_000 });
  await expect(
    page.getByText(TEST_TAGS.player, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
});
