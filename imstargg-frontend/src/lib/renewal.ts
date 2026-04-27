// Renewal state machine — client-only singleton with subscriptions.
// Status: IDLE | RENEWING | COOLDOWN | JUST_DONE | ERROR

export type RenewalStatus = "IDLE" | "RENEWING" | "COOLDOWN" | "JUST_DONE" | "ERROR";

export interface RenewalState {
  status: RenewalStatus;
  canRenew: boolean;
  cooldownSecondsLeft: number;
  progressPct: number;
  lastRenewedAt: string;
}

let state: RenewalState = {
  status: "IDLE",
  canRenew: true,
  cooldownSecondsLeft: 0,
  progressPct: 0,
  lastRenewedAt: "2분 전",
};
const listeners = new Set<(s: RenewalState) => void>();

export function getRenewalState(): RenewalState {
  return state;
}

export function subscribeRenewal(fn: (s: RenewalState) => void): () => void {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}

function setState(patch: Partial<RenewalState>) {
  state = { ...state, ...patch };
  listeners.forEach((fn) => fn(state));
}

function startCooldown() {
  const tick = setInterval(() => {
    const next = state.cooldownSecondsLeft - 1;
    if (next <= 0) {
      clearInterval(tick);
      setState({ status: "IDLE", canRenew: true, cooldownSecondsLeft: 0 });
    } else {
      setState({ cooldownSecondsLeft: next });
    }
  }, 1000);
}

export function startRenewal() {
  if (state.status === "RENEWING" || !state.canRenew) return;
  setState({ status: "RENEWING", progressPct: 5 });
  let p = 5;
  const tick = setInterval(() => {
    p += 5 + Math.random() * 8;
    if (p >= 100) {
      clearInterval(tick);
      setState({ status: "JUST_DONE", progressPct: 100, lastRenewedAt: "방금 전" });
      setTimeout(() => {
        setState({
          status: "COOLDOWN",
          canRenew: false,
          cooldownSecondsLeft: 600,
          progressPct: 0,
        });
        startCooldown();
      }, 1500);
    } else {
      setState({ progressPct: Math.min(99, p) });
    }
  }, 200);
}

export function triggerRenewalError(): string {
  setState({ status: "ERROR" });
  setTimeout(() => setState({ status: "IDLE" }), 100);
  return "서버에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
}
