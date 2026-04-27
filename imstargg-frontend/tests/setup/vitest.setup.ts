import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll, vi } from "vitest";
import { server } from "./msw";

// jsdom provides its own AbortSignal that Node's undici Request constructor
// rejects with "Expected signal to be an instance of AbortSignal" (cross-realm).
// Drop the incoming signal — abort propagation isn't needed in tests.
function installFetchSignalShim() {
  if (typeof window === "undefined") return;
  const originalFetch = globalThis.fetch.bind(globalThis);
  const wrapped: typeof fetch = (input, init) => {
    if (init?.signal) {
      const { signal: _drop, ...rest } = init;
      return originalFetch(input, rest);
    }
    return originalFetch(input, init);
  };
  globalThis.fetch = wrapped;
  Object.defineProperty(window, "fetch", { writable: true, value: wrapped });
}

if (typeof window !== "undefined" && !window.matchMedia) {
  Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

beforeAll(() => {
  server.listen({ onUnhandledRequest: "error" });
  // Install AFTER MSW patches fetch so we strip jsdom's AbortSignal
  // before MSW's interceptor forwards init to Node's Request constructor.
  installFetchSignalShim();
});
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
