import { test, expect } from "@playwright/test";

test("/robots.txt 가 사이트맵을 가리킨다", async ({ request }) => {
  const res = await request.get("/robots.txt");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toMatch(/Sitemap:/);
});

test("/sitemap.xml 이 정적 라우트를 포함한다", async ({ request }) => {
  const res = await request.get("/sitemap.xml");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toContain("/players");
  expect(body).toContain("/clubs");
  expect(body).toContain("/brawlers");
  expect(body).toContain("/events");
});

test("플레이어 페이지의 canonical 링크가 자기 자신을 가리킨다", async ({ page }) => {
  await page.goto("/players/%2388J8Q9RPV");
  const canonical = await page.locator('link[rel="canonical"]').getAttribute("href");
  expect(canonical ?? "").toContain("/players/%2388J8Q9RPV");
});
