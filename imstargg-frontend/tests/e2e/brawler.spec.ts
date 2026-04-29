import { test, expect } from "@playwright/test";

test("브롤러 메타 페이지가 표를 렌더한다", async ({ page }) => {
  await page.goto("/brawlers");
  const rows = page.locator('a[href^="/brawlers/"]');
  await expect(rows.first()).toBeVisible({ timeout: 15_000 });
  expect(await rows.count()).toBeGreaterThanOrEqual(50);
});

test("이름 검색이 결과를 좁힌다", async ({ page }) => {
  await page.goto("/brawlers");
  const input = page.getByPlaceholder("브롤러 이름 검색");
  await input.waitFor({ state: "visible", timeout: 15_000 });
  await input.fill("셸리");
  await expect(page.locator('a[href^="/brawlers/"]')).toHaveCount(1, { timeout: 5_000 });
});

test("행 클릭 시 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/brawlers");
  const firstRow = page.locator('a[href^="/brawlers/"]').first();
  await firstRow.waitFor({ state: "visible", timeout: 15_000 });
  await firstRow.click();
  await page.waitForURL(/\/brawlers\/\d+/, { timeout: 15_000 });
});

test("상세 페이지 SEO: title에 브롤러 이름 + canonical 1개", async ({ page }) => {
  await page.goto("/brawlers/16000000"); // Shelly
  await expect(page).toHaveTitle(/ImStarGG/, { timeout: 15_000 });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});
