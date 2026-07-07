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
      const { signal, ...rest } = init;
      void signal;
      return originalFetch(input, rest);
    }
    return originalFetch(input, init);
  };
  globalThis.fetch = wrapped;
  Object.defineProperty(window, "fetch", { writable: true, value: wrapped });
}

// Node 23+ ships its own Web Storage on globalThis (localStorage stays
// undefined unless --localstorage-file is given). vitest's jsdom environment
// skips window keys that already exist on the global, so tests would see
// Node's storage instead of jsdom's. Re-point both at the jsdom window.
function installJsdomStorage() {
  const win = (globalThis as { jsdom?: { window: Window } }).jsdom?.window;
  if (!win) return;
  for (const key of ["localStorage", "sessionStorage"] as const) {
    if (globalThis[key] !== win[key]) {
      Object.defineProperty(globalThis, key, {
        configurable: true,
        enumerable: true,
        get: () => win[key],
      });
    }
  }
}
installJsdomStorage();

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
