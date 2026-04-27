// TODO(domain-spec): Replace mock data with real API. See docs/superpowers/specs/2026-04-27-frontend-foundation-design.md §15.
import ClubDetailPage from "./_components/ClubDetailPage";

interface Props {
  params: Promise<{ tag: string }>;
}

export default async function Page({ params }: Props) {
  const { tag } = await params;
  return <ClubDetailPage tag={tag} />;
}
