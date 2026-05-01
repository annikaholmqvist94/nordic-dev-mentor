import { ChatClient } from "./ChatClient";

export default async function Home({
  searchParams,
}: {
  searchParams: Promise<{ id?: string }>;
}) {
  const params = await searchParams;
  return <ChatClient initialSessionId={params.id} />;
}
