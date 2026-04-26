import ClubDetailPage from "./_components/ClubDetailPage";

interface Props {
  params: Promise<{ tag: string }>;
}

export default async function Page({ params }: Props) {
  const { tag } = await params;
  return <ClubDetailPage tag={tag} />;
}
