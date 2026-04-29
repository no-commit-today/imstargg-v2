import { ImageResponse } from "next/og";
import { clubApi } from "@/api/endpoints/club";
import { NotFoundError } from "@/api/errors";
import { formatNumber } from "@/lib/format";
import { loadOgFonts } from "@/lib/og/loadOgFonts";
import { OgCard } from "@/lib/og/OgCard";

export const clubOgAlt = "ImStarGG 클럽 카드";
export const clubOgSize = { width: 1200, height: 630 } as const;
export const clubOgContentType = "image/png";

export async function clubOgImage(tag: string): Promise<Response> {
  const fonts = await loadOgFonts();

  try {
    const dto = await clubApi.get(tag, { next: { revalidate: 60 } });
    return new ImageResponse(
      (
        <OgCard
          eyebrow="CLUB"
          name={dto.name}
          tag={dto.tag}
          stats={[
            { label: "트로피", value: formatNumber(dto.trophies) },
            { label: "멤버", value: `${dto.members.length}/30` },
          ]}
        />
      ),
      { ...clubOgSize, fonts },
    );
  } catch (e) {
    if (e instanceof NotFoundError) {
      return new ImageResponse(
        (
          <OgCard
            eyebrow="CLUB"
            name="클럽을 찾을 수 없음"
            tag={tag}
            stats={[]}
          />
        ),
        { ...clubOgSize, fonts },
      );
    }
    throw e;
  }
}
