import { TopBar } from "../components/TopBar";
import { HistoryClient } from "./HistoryClient";

export default function HistoryPage() {
  return (
    <div className="flex flex-col min-h-screen">
      <TopBar />
      <main className="flex-1 px-8 py-9">
        <div className="max-w-[580px] mx-auto">
          <h1 className="font-serif text-[28px] font-medium leading-[1.2] mb-8">
            Conversations
          </h1>
          <HistoryClient />
        </div>
      </main>
    </div>
  );
}
