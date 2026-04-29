import { expect, test } from "@playwright/test";

test("/brawlers 메타 표가 렌더된다", async ({ page }) => {
  await page.goto("/brawlers");
  await expect(page.getByRole("heading", { name: "브롤러 메타" })).toBeVisible({
    timeout: 15_000,
  });
  // 첫 번째 데이터 행이 등장
  const firstRow = page.locator('a[href^="/brawlers/"]').first();
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
});

test("/brawlers tier 변경 시 URL 갱신", async ({ page }) => {
  await page.goto("/brawlers");
  await page.getByRole("combobox").first().click();
  await page.getByRole("option", { name: "트로피 2,000+" }).click();
  await expect(page).toHaveURL(/tier=TROPHY_2000_PLUS/);
});

test("/brawlers/[id] 상세 통계가 모두 렌더된다", async ({ page }) => {
  await page.goto("/brawlers");
  const firstLink = page.locator('a[href^="/brawlers/"]').first();
  await firstLink.click();
  await expect(page.getByText("일별 트렌드")).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText("페어 통계")).toBeVisible();
  await expect(page.getByText("적군 통계")).toBeVisible();
  await expect(page.getByText("아이템 보유율")).toBeVisible();
});

test("/events/[id] RESULT 모드: 행 클릭 시 brawlerId URL", async ({ page }) => {
  // event id 는 rotation 첫번째로 가져온다
  await page.goto("/events");
  const eventLink = page.locator('a[href^="/events/"]').first();
  const href = await eventLink.getAttribute("href");
  if (!href) test.skip();
  await page.goto(href!);
  await expect(page.getByRole("heading", { name: /이벤트 통계/ })).toBeVisible({
    timeout: 15_000,
  });
  const firstStatRow = page.locator('button:has(img[alt])').first();
  await firstStatRow.click();
  await expect(page).toHaveURL(/brawlerId=\d+/);
  await expect(page.getByText("선택한 브롤러 분석")).toBeVisible();
});

test("페어 표 헤더 클릭 시 정렬 URL 변경", async ({ page }) => {
  await page.goto("/brawlers");
  const firstLink = page.locator('a[href^="/brawlers/"]').first();
  await firstLink.click();
  await expect(page.getByText("페어 통계")).toBeVisible({ timeout: 15_000 });
  await page.getByRole("button", { name: /^승률/ }).first().click();
  await expect(page).toHaveURL(/pairSort=WIN_RATE/);
});

test("/brawlers/[id] date 변경 시 URL 갱신", async ({ page }) => {
  await page.goto("/brawlers/16000000");
  // 두 번째 셀렉터(date)
  const selectors = page.getByRole("combobox");
  await expect(selectors.nth(1)).toBeVisible({ timeout: 15_000 });
  await selectors.nth(1).click();
  await page.getByRole("option", { name: "지난 주", exact: true }).click();
  await expect(page).toHaveURL(/date=LAST_WEEK/);
});
