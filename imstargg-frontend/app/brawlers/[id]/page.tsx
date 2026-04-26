import BrawlerDetailPage from "./_components/BrawlerDetailPage";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function Page({ params }: Props) {
  const { id } = await params;
  return <BrawlerDetailPage id={id} />;
}
