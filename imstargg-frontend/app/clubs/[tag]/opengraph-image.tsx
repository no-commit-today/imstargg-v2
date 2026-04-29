import {
  clubOgAlt,
  clubOgContentType,
  clubOgImage,
  clubOgSize,
} from "@/lib/og/clubOgImage";

export const alt = clubOgAlt;
export const size = clubOgSize;
export const contentType = clubOgContentType;

type Props = { params: Promise<{ tag: string }> };

export default async function Image({ params }: Props) {
  const { tag } = await params;
  return clubOgImage(tag);
}
