import type {
  DashboardSummary,
  BrawlerListItem,
  BrawlerDetail,
  BuffyType,
  Gear,
  BattleEvent,
  BattleEventMode,
  BattleEventModeResultType,
  Maintenance,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(options?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...options?.headers,
    },
  });
  if (!res.ok) {
    throw new Error(`API error: ${res.status} ${res.statusText}`);
  }
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text);
}

// Dashboard
export const getDashboardSummary = () => request<DashboardSummary>("/dashboard/summary");

// Brawler
export const getBrawlers = () => request<BrawlerListItem[]>("/brawlers");

export const getBrawlerDetail = (brawlStarsId: number) =>
  request<BrawlerDetail>(`/brawlers/${brawlStarsId}`);

export const createBrawler = (data: { brawlStarsId: number; rarity: string; role: string }) =>
  request<void>("/brawlers", { method: "POST", body: JSON.stringify(data) });

export const updateBrawlerMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/brawlers/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadBrawlerImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/brawlers/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

export const createBrawlerBuffy = (brawlStarsId: number, type: BuffyType) =>
  request<void>(`/brawlers/${brawlStarsId}/buffies`, {
    method: "POST",
    body: JSON.stringify({ type }),
  });

export const uploadBrawlerBuffyImage = (brawlStarsId: number, type: BuffyType, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/brawlers/${brawlStarsId}/buffies/${type}/image`, {
    method: "PUT",
    body: formData,
  });
};

export const deleteBrawlerBuffy = (brawlStarsId: number, type: BuffyType) =>
  request<void>(`/brawlers/${brawlStarsId}/buffies/${type}`, { method: "DELETE" });

export const updateBrawlerGears = (brawlStarsId: number, gearBrawlStarsIds: number[]) =>
  request<void>(`/brawlers/${brawlStarsId}/gears`, {
    method: "PUT",
    body: JSON.stringify({ gearBrawlStarsIds }),
  });

export const createStarPower = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/star-powers`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateStarPowerMessages = (
  brawlerBrawlStarsId: number,
  starPowerBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/star-powers/${starPowerBrawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadStarPowerImage = (
  brawlerBrawlStarsId: number,
  starPowerBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/star-powers/${starPowerBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

export const createGadget = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/gadgets`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateGadgetMessages = (
  brawlerBrawlStarsId: number,
  gadgetBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/gadgets/${gadgetBrawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadGadgetImage = (
  brawlerBrawlStarsId: number,
  gadgetBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/gadgets/${gadgetBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

export const createHyperCharge = (brawlerBrawlStarsId: number, brawlStarsId: number) =>
  request<void>(`/brawlers/${brawlerBrawlStarsId}/hyper-charges`, {
    method: "POST",
    body: JSON.stringify({ brawlStarsId }),
  });

export const updateHyperChargeMessages = (
  brawlerBrawlStarsId: number,
  hyperChargeBrawlStarsId: number,
  messages: Record<string, string>,
) =>
  request<void>(
    `/brawlers/${brawlerBrawlStarsId}/hyper-charges/${hyperChargeBrawlStarsId}/messages`,
    { method: "PUT", body: JSON.stringify({ messages }) },
  );

export const uploadHyperChargeImage = (
  brawlerBrawlStarsId: number,
  hyperChargeBrawlStarsId: number,
  file: File,
) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(
    `/brawlers/${brawlerBrawlStarsId}/hyper-charges/${hyperChargeBrawlStarsId}/image`,
    { method: "PUT", body: formData },
  );
};

// Gear
export const getGears = () => request<Gear[]>("/gears");

export const createGear = (data: {
  brawlStarsId: number;
  rarity: string;
  names: Record<string, string>;
}) => request<void>("/gears", { method: "POST", body: JSON.stringify(data) });

export const updateGearMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/gears/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadGearImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/gears/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

// Event
export const getEvents = () => request<BattleEvent[]>("/events");

export const createEvent = (data: { brawlStarsId: number; modeId?: number; mapName?: string }) =>
  request<void>("/events", { method: "POST", body: JSON.stringify(data) });

export const updateEventMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/events/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

// Event Mode
export const getEventModes = () => request<BattleEventMode[]>("/event-modes");

export const createEventMode = (data: {
  brawlStarsId: number;
  brawlStarsName: string;
  resultType?: BattleEventModeResultType;
}) => request<void>("/event-modes", { method: "POST", body: JSON.stringify(data) });

export const updateEventModeMessages = (brawlStarsId: number, messages: Record<string, string>) =>
  request<void>(`/event-modes/${brawlStarsId}/messages`, {
    method: "PUT",
    body: JSON.stringify({ messages }),
  });

export const uploadEventModeImage = (brawlStarsId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return request<void>(`/event-modes/${brawlStarsId}/image`, { method: "PUT", body: formData });
};

export const updateEventMode = (
  brawlStarsId: number,
  data: { resultType: BattleEventModeResultType; playerCounts: number[] },
) =>
  request<void>(`/event-modes/${brawlStarsId}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });

export const updateEventModeColor = (brawlStarsId: number, color: string) =>
  request<void>(`/event-modes/${brawlStarsId}/color`, {
    method: "PUT",
    body: JSON.stringify({ color }),
  });

// Maintenance
export const getMaintenances = () => request<Maintenance[]>("/maintenances");

export const registerMaintenance = (data: { startTime: string; endTime: string }) =>
  request<void>("/maintenances", { method: "POST", body: JSON.stringify(data) });

export const deleteMaintenance = (id: number) =>
  request<void>(`/maintenances/${id}`, { method: "DELETE" });
