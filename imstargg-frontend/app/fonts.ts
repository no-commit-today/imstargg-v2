import { Bricolage_Grotesque, JetBrains_Mono, Lilita_One } from "next/font/google";
import localFont from "next/font/local";

export const bricolage = Bricolage_Grotesque({
  subsets: ["latin"],
  axes: ["opsz"],
  variable: "--font-bricolage",
  display: "swap",
});

export const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains",
  display: "swap",
});

export const lilitaOne = Lilita_One({
  subsets: ["latin"],
  weight: "400",
  variable: "--font-lilita",
  display: "swap",
});

export const koBrawl = localFont({
  src: "./fonts/KoBrawlGothic-subset.woff2",
  weight: "400",
  variable: "--font-kobrawl",
  display: "swap",
});

export const pretendard = localFont({
  src: "./fonts/PretendardVariable-subset.woff2",
  weight: "45 930",
  variable: "--font-pretendard",
  display: "swap",
});
