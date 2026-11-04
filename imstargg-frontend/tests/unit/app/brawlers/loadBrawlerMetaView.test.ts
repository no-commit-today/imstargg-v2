import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../../setup/msw";
import { loadBrawlerMetaView } from "@app/brawlers/_lib/loadBrawlerMetaView";

function recordAll(urls: string[]) {
  server.use(
    http.get("*/v1/statistics/pair/result", ({ request }) => {
      urls.push(new URL(request.url).pathname);
      return HttpResponse.json({ content: [] });
    }),
    http.get("*/v1/statistics/trio/result", ({ request }) => {
      urls.push(new URL(request.url).pathname);
      return HttpResponse.json({ content: [] });
    }),
    http.get("*/v1/statistics/brawler/result", ({ request }) => {
      urls.push(new URL(request.url).pathname);
      return HttpResponse.json({ totalBattleCount: 0, items: [] });
    }),
    http.get("*/v1/brawlers", ({ request }) => {
      urls.push(new URL(request.url).pathname);
      return HttpResponse.json({ content: [] });
    }),
  );
}

describe("loadBrawlerMetaView", () => {
  it("view=pair 면 /v1/statistics/pair/result 만 호출하고 brawler/result 는 부르지 않는다", async () => {
    const urls: string[] = [];
    recordAll(urls);

    const data = await loadBrawlerMetaView("pair", "TROPHY_1000_PLUS", "SINCE_LAST_WEEK");

    expect(urls).toEqual(["/v1/statistics/pair/result"]);
    expect(data).toEqual({ view: "pair", items: [] });
  });

  it("view=trio 면 /v1/statistics/trio/result 만 호출한다", async () => {
    const urls: string[] = [];
    recordAll(urls);

    const data = await loadBrawlerMetaView("trio", "TROPHY_1000_PLUS", "SINCE_LAST_WEEK");

    expect(urls).toEqual(["/v1/statistics/trio/result"]);
    expect(data).toEqual({ view: "trio", items: [] });
  });

  it("view=brawler 면 brawler/result 와 brawler 목록을 부른다", async () => {
    const urls: string[] = [];
    recordAll(urls);

    const data = await loadBrawlerMetaView("brawler", "TROPHY_1000_PLUS", "SINCE_LAST_WEEK");

    expect([...urls].sort()).toEqual(["/v1/brawlers", "/v1/statistics/brawler/result"]);
    expect(data.view).toBe("brawler");
    if (data.view === "brawler") {
      expect(data.stats.totalBattleCount).toBe(0);
      expect(data.brawlers).toEqual([]);
    }
  });
});
