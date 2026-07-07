import { test, expect } from "@playwright/test";

// 유효한 태그 문자셋(0289PYLQGRJCUV)이지만 실존하지 않는 태그 →
// core-api 404 → PlayerNotFoundView (콘텐츠 없는 화면)
const UNKNOWN_TAG_ENCODED = "%23RRRRRRRRR";

test("미갱신 플레이어 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto(`/players/${UNKNOWN_TAG_ENCODED}`);
  await expect(page.getByText("플레이어를 찾을 수 없어요")).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});

test("404 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto("/this-page-does-not-exist");
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});

test("빈 검색 페이지에는 광고 요소가 없다", async ({ page }) => {
  await page.goto("/players/search");
  await expect(page.locator("ins.adsbygoogle")).toHaveCount(0);
  await expect(page.locator('script[src*="adsbygoogle"]')).toHaveCount(0);
});
