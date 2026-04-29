import {
  playerOgAlt,
  playerOgContentType,
  playerOgImage,
  playerOgSize,
} from "@/lib/og/playerOgImage";

export const alt = playerOgAlt;
export const size = playerOgSize;
export const contentType = playerOgContentType;

type Props = { params: Promise<{ tag: string }> };

export default async function Image({ params }: Props) {
  const { tag } = await params;
  return playerOgImage(tag);
}
