import { ImageResponse } from "next/og";
import { playerApi } from "@/api/endpoints/player";
import { NotFoundError } from "@/api/errors";
import { formatNumber } from "@/lib/format";
import { loadOgFonts } from "@/lib/og/loadOgFonts";
import { OgCard } from "@/lib/og/OgCard";

export const playerOgAlt = "ImStarGG 플레이어 카드";
export const playerOgSize = { width: 1200, height: 630 } as const;
export const playerOgContentType = "image/png";

export async function playerOgImage(tag: string): Promise<Response> {
  const fonts = await loadOgFonts();

  try {
    const dto = await playerApi.get(tag, { next: { revalidate: 60 } });
    return new ImageResponse(
      (
        <OgCard
          eyebrow="PLAYER"
          name={dto.name}
          tag={dto.tag}
          stats={[
            { label: "트로피", value: formatNumber(dto.trophies) },
            { label: "클럽", value: dto.club?.name ?? "클럽 없음" },
          ]}
        />
      ),
      { ...playerOgSize, fonts },
    );
  } catch (e) {
    if (e instanceof NotFoundError) {
      return new ImageResponse(
        (
          <OgCard
            eyebrow="PLAYER"
            name="플레이어를 찾을 수 없음"
            tag={tag}
            stats={[]}
          />
        ),
        { ...playerOgSize, fonts },
      );
    }
    throw e;
  }
}
