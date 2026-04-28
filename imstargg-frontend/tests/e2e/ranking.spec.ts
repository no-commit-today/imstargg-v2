import { expect, test } from "@playwright/test";

test.describe("Ranking domain", () => {
  test("플레이어 랭킹 글로벌 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/ranking/players");
    await expect(
      page.getByRole("heading", { name: /글로벌 플레이어 랭킹/ }),
    ).toBeVisible();
    await expect(page.getByLabel("국가 선택")).toBeVisible();
  });

  test("CountrySelector로 국가를 전환하면 URL이 바뀐다", async ({ page }) => {
    await page.goto("/ranking/players");
    await page.getByLabel("국가 선택").click();
    await page.getByRole("option", { name: "한국" }).click();
    await page.waitForURL(/country=kr/);
    await expect(
      page.getByRole("heading", { name: /한국 플레이어 랭킹/ }),
    ).toBeVisible();
  });

  test("클럽 랭킹 페이지가 렌더링된다", async ({ page }) => {
    await page.goto("/ranking/clubs?country=kr");
    await expect(
      page.getByRole("heading", { name: /한국 클럽 랭킹/ }),
    ).toBeVisible();
  });

  test("브롤러 그리드에서 브롤러를 선택하면 랭킹 페이지로 이동한다", async ({
    page,
  }) => {
    await page.goto("/ranking/brawlers");
    await expect(
      page.getByRole("heading", { name: "브롤러 랭킹" }),
    ).toBeVisible();
    const firstLink = page
      .locator('a[href^="/ranking/brawlers/"]')
      .first();
    await firstLink.click();
    await page.waitForURL(/\/ranking\/brawlers\/\d+/);
    await expect(page.getByLabel("국가 선택")).toBeVisible();
  });

  test("/ranking 인덱스 페이지가 3개 카드를 표시한다", async ({ page }) => {
    await page.goto("/ranking");
    await expect(
      page.getByRole("heading", { name: "플레이어 랭킹" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "브롤러 랭킹" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "클럽 랭킹" }),
    ).toBeVisible();
  });
});
