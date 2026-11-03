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
  // 카드를 최대 5장 넘기며 장당 최대 15s 기다리므로 기본 30s 로는 반복 도중 시간 초과가 나 실패 메시지가 묻힌다.
  test.setTimeout(90_000);
  // 통계 미지원 모드의 이벤트는 통계 대신 안내를 보이므로, 안내가 보이면 다음 카드로 넘어간다 (최대 5장).
  // 결과형/순위형 모드는 공통 동작이라 어느 쪽이든 상관없다.
  await page.goto("/events");
  const eventLinks = page.locator('a[href^="/events/"]');
  await eventLinks.first().waitFor();
  const hrefs = (
    await eventLinks.evaluateAll((links) => links.slice(0, 5).map((a) => a.getAttribute("href")))
  ).filter((href): href is string => href !== null);
  const statsHeading = page.getByRole("heading", { name: /이벤트 통계/ });
  const unsupportedNotice = page.getByText("이 모드는 통계를 제공하지 않습니다");
  let found = false;
  for (const href of hrefs) {
    await page.goto(href);
    await expect(statsHeading.or(unsupportedNotice)).toBeVisible({ timeout: 15_000 });
    if (await statsHeading.isVisible()) {
      found = true;
      break;
    }
  }
  expect(found, "통계를 제공하는 모드의 이벤트가 앞 5장 안에 있어야 한다").toBe(true);
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
