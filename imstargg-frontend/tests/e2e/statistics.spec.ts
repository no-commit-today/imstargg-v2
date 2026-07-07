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
  await expect(page.getByRole("heading", { name: "일별 트렌드" })).toBeVisible({
    timeout: 15_000,
  });
  await expect(page.getByRole("heading", { name: "조합" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "카운터" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "아이템" })).toBeVisible();
});

test("/events/[id] 통계 행 클릭 시 brawlerId URL + 선택 패널", async ({ page }) => {
  // event id 는 rotation 첫번째로 가져온다 (RESULT/RANK 모드 공통 동작)
  await page.goto("/events");
  const eventLink = page.locator('a[href^="/events/"]').first();
  const href = await eventLink.getAttribute("href");
  if (!href) test.skip();
  await page.goto(href!);
  await expect(page.getByRole("heading", { name: /이벤트 통계/ })).toBeVisible({
    timeout: 15_000,
  });
  // 통계 행은 aria-pressed 토글 버튼 (button:has(img) 는 TopNav 로고와 충돌)
  const firstStatRow = page.locator("button[aria-pressed]").first();
  await firstStatRow.click();
  await expect(page).toHaveURL(/brawlerId=\d+/);
  await expect(
    page.getByRole("heading", { name: "선택한 브롤러 분석" }),
  ).toBeVisible({ timeout: 15_000 });
});

test("조합 표 승률 헤더 클릭 시 정렬 방향이 토글된다", async ({ page }) => {
  // 정렬은 URL 동기화 없이 클라이언트 상태로만 동작한다 (커밋 6a74115e)
  await page.goto("/brawlers/16000000");
  const pairTable = page
    .locator("div.data-table")
    .filter({ has: page.getByRole("heading", { name: "조합" }) });
  const winRateHeader = pairTable.getByRole("button", { name: "승률" });
  await expect(winRateHeader).toBeVisible({ timeout: 15_000 });
  await expect(winRateHeader).toContainText("▼"); // 초기 정렬: 승률 DESC
  await winRateHeader.click();
  await expect(winRateHeader).toContainText("▲");
});

test("/brawlers/[id] date 변경 시 URL 갱신", async ({ page }) => {
  await page.goto("/brawlers/16000000");
  const dateSelect = page.getByRole("combobox", { name: "기간 선택" });
  await expect(dateSelect).toBeVisible({ timeout: 15_000 });
  await dateSelect.click();
  await page.getByRole("option", { name: "지난 주", exact: true }).click();
  await expect(page).toHaveURL(/date=LAST_WEEK/);
});
