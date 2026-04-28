import { test, expect } from "@playwright/test";

test("이벤트 목록은 카드를 렌더한다", async ({ page }) => {
  await page.goto("/events");
  const cards = page.locator('a[href^="/events/"]');
  await expect(cards.first()).toBeVisible({ timeout: 15_000 });
  expect(await cards.count()).toBeGreaterThanOrEqual(1);
});

test("rotation 카드는 카운트다운을 표시한다", async ({ page }) => {
  await page.goto("/events");
  const card = page.locator('a[href^="/events/"]').first();
  await card.waitFor({ state: "visible", timeout: 15_000 });
  // HH:MM:SS 형태 또는 종료됨
  await expect(card).toContainText(/\d{2}:\d{2}:\d{2}|종료됨/, { timeout: 5_000 });
});

test("맵 이름 검색은 결과를 표시한다", async ({ page }) => {
  await page.goto("/events");
  const cardSelector = 'a[href^="/events/"]';
  await page.locator(cardSelector).first().waitFor({ state: "visible", timeout: 15_000 });

  // 첫 카드의 맵 이름을 추출해 그 안의 한 토큰으로 검색한다
  const firstCardText = (await page.locator(cardSelector).first().innerText()).trim();
  // 한글/영문 토큰 추출 (공백 분리, 길이 ≥2)
  const token = firstCardText
    .split(/\s+/)
    .map((s) => s.replace(/[^\p{L}\p{N}]/gu, ""))
    .find((s) => s.length >= 2);
  test.skip(!token, "맵 이름에서 검색 토큰을 찾지 못했어요");

  const input = page.getByLabel("맵 이름 검색");
  await input.fill(token!);
  await expect(page.locator(cardSelector).first()).toBeVisible({ timeout: 5_000 });
});

test("검색을 비우면 메인 섹션으로 돌아간다", async ({ page }) => {
  await page.goto("/events");
  const input = page.getByLabel("맵 이름 검색");
  await input.fill("zzz_no_match_zzz");
  await expect(page.getByText("결과 없음")).toBeVisible({ timeout: 5_000 });
  await input.fill("");
  await expect(page.getByRole("heading", { name: "현재 진행 중" })).toBeVisible({
    timeout: 5_000,
  });
});

test("카드 클릭 시 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/events");
  const firstCard = page.locator('a[href^="/events/"]').first();
  await firstCard.waitFor({ state: "visible", timeout: 15_000 });
  await firstCard.click();
  await page.waitForURL(/\/events\/\d+/, { timeout: 15_000 });
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
});

test("상세 페이지 SEO: title + canonical 1개", async ({ page }) => {
  // 목록에서 첫 이벤트 ID 추출
  await page.goto("/events");
  const firstCard = page.locator('a[href^="/events/"]').first();
  await firstCard.waitFor({ state: "visible", timeout: 15_000 });
  const href = await firstCard.getAttribute("href");
  test.skip(!href, "이벤트 카드 href를 찾지 못했어요");

  await page.goto(href!);
  await expect(page).toHaveTitle(/ImStarGG/, { timeout: 15_000 });
  const canonical = page.locator('link[rel="canonical"]');
  await expect(canonical).toHaveCount(1);
});
