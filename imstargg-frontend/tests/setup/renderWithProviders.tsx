import { render } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";
import type { ReactElement } from "react";

export function renderWithProviders(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0, staleTime: 0 } },
  });
  const view = render(
    <QueryClientProvider client={queryClient}>
      {ui}
      <Toaster richColors position="top-center" />
    </QueryClientProvider>,
  );
  return { ...view, queryClient };
}
