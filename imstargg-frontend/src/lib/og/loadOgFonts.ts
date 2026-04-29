import { readFile } from "node:fs/promises";
import { join } from "node:path";

export interface OgFont {
  name: string;
  data: Buffer;
  style: "normal";
  weight: 400;
}

export async function loadOgFonts(): Promise<OgFont[]> {
  const [koBrawl, lilita] = await Promise.all([
    readFile(join(process.cwd(), "public/fonts/KoBrawl_Gothic40.otf")),
    readFile(join(process.cwd(), "public/fonts/lilitaone-regular-webfont__2_.ttf")),
  ]);
  return [
    { name: "KoBrawl Gothic", data: koBrawl, style: "normal", weight: 400 },
    { name: "Lilita One", data: lilita, style: "normal", weight: 400 },
  ];
}
