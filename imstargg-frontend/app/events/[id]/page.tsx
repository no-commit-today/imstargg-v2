import EventDetailPage from "./_components/EventDetailPage";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function Page({ params }: Props) {
  const { id } = await params;
  return <EventDetailPage id={id} />;
}
