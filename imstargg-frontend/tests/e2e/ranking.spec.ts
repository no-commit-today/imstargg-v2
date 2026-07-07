import { expect, test } from "@playwright/test";

test.describe("Ranking domain", () => {
  test("플레이어 랭킹 글로벌 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/players");
    await expect(
      page.getByRole("heading", { name: /글로벌 랭킹/ }),
    ).toBeVisible();
    await expect(page.getByRole("combobox", { name: "국가 선택" })).toBeVisible();
  });

  test("국가 셀렉트로 국가를 전환하면 URL이 바뀐다", async ({ page }) => {
    await page.goto("/players");
    await page.getByRole("combobox", { name: "국가 선택" }).click();
    await page.getByRole("option", { name: "한국" }).click();
    await page.waitForURL(/country=kr/);
    await expect(
      page.getByRole("heading", { name: /한국 랭킹/ }),
    ).toBeVisible();
  });

  test("클럽 랭킹 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/clubs?country=kr");
    await expect(
      page.getByRole("heading", { name: /한국 랭킹/ }),
    ).toBeVisible();
  });
});
