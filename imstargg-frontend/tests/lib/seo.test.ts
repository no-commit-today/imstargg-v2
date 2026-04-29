import { describe, expect, it } from "vitest";
import { buildMetadata } from "@/lib/seo";

describe("buildMetadata", () => {
  const base = {
    title: "T",
    description: "D",
    path: "/p",
  } as const;

  it("dynamicOg 미지정 시 openGraph.images 와 twitter.images 가 hasOwnProperty 로 존재한다", () => {
    const meta = buildMetadata(base);
    expect(meta.openGraph).toBeDefined();
    expect(meta.twitter).toBeDefined();
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(true);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(true);
    expect(meta.openGraph!.images).toEqual([
      { url: "/assets/logo-full.png", width: 1200, height: 630 },
    ]);
    expect(meta.twitter!.images).toEqual(["/assets/logo-full.png"]);
  });

  it("dynamicOg=true 일 때 openGraph 와 twitter 객체에서 images 키가 omit 된다", () => {
    const meta = buildMetadata({ ...base, dynamicOg: true });
    expect(meta.openGraph).toBeDefined();
    expect(meta.twitter).toBeDefined();
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(false);
  });

  it("dynamicOg=true + image 옵션이 와도 images 는 omit 한다", () => {
    const meta = buildMetadata({ ...base, dynamicOg: true, image: "/x.png" });
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(false);
  });
});
