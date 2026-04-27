// TODO(domain-spec): Replace mock data with real API. See docs/superpowers/specs/2026-04-27-frontend-foundation-design.md §15.
import BrawlerDetailPage from "./_components/BrawlerDetailPage";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function Page({ params }: Props) {
  const { id } = await params;
  return <BrawlerDetailPage id={id} />;
}
