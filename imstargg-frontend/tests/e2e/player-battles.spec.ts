import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("배틀 탭 클릭 시 배틀 행이 렌더된다", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  // 첫 행이 보이면 OK (행 컴포넌트는 role="button" + aria-expanded)
  const firstRow = page.locator('[role="button"][aria-expanded]').first();
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
});

test("배틀 행 클릭 시 펼침 동작", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  const firstRow = page.locator('[role="button"][aria-expanded]').first();
  await firstRow.waitFor({ state: "visible", timeout: 15_000 });
  await expect(firstRow).toHaveAttribute("aria-expanded", "false");
  await firstRow.click();
  await expect(firstRow).toHaveAttribute("aria-expanded", "true");
});

test("'더 보기' 버튼 클릭 시 추가 행 append", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await page.getByRole("tab", { name: "배틀" }).click();
  const moreBtn = page.getByRole("button", { name: "더 보기" });
  // 더 보기 버튼이 없으면 (배틀이 25개 미만) 스킵
  if (!(await moreBtn.isVisible({ timeout: 15_000 }).catch(() => false))) {
    test.skip(true, "테스트 플레이어의 배틀이 25개 미만");
  }
  const before = await page.locator('[role="button"][aria-expanded]').count();
  await moreBtn.click();
  await expect
    .poll(() => page.locator('[role="button"][aria-expanded]').count(), { timeout: 15_000 })
    .toBeGreaterThan(before);
});
