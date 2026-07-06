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

test("플레이어 태그 검색 후 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/");
  const input = page.getByLabel("플레이어 이름 또는 태그 입력");
  await input.fill(TEST_TAGS.player);
  await page.getByRole("button", { name: "플레이어 검색" }).click();
  await expect(page).toHaveURL(new RegExp(TEST_TAGS.playerEncoded));
  await expect(
    page.getByText(TEST_TAGS.player, { exact: true })
  ).toBeVisible({ timeout: 15_000 });
});
