const API_URL = import.meta.env.VITE_API_URL ?? "";
const DEFAULT_WS_URL = `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}`;
export const WS_URL = (import.meta.env.VITE_WS_URL ?? DEFAULT_WS_URL) + "/ws";

export type Account = {
  id: string;
  institutionCode: string;
  institutionName: string;
  maskedAccountNumber: string;
  accountHolder: string;
  alias?: string;
  primary: boolean;
};
export type Institution = { code: string; name: string; category: "BANK" | "SECURITIES"; logoUrl: string };
export type History = {
  id: string;
  title: string;
  totalAmount: number;
  status: string;
  role: string;
  paymentStatus: string;
  createdAt: string;
};
export type AuditLog = {
  eventType: string;
  actorName: string;
  beforeData?: Record<string, unknown>;
  afterData?: Record<string, unknown>;
  createdAt: string;
};
export type Participant = {
  participantId: string;
  userId: string;
  userName: string;
  amount: number;
  role: "HOST" | "MEMBER";
  paymentStatus: string;
  version: number;
};
export type Snapshot = {
  roomId: string;
  inviteUrl: string;
  status: string;
  recipient: { institutionCode: string; institutionName: string; accountNumber: string; accountHolder: string };
  totalAmount: number;
  allocatedAmount: number;
  remainingAmount: number;
  participants: Participant[];
};
export type Room = {
  id: string;
  title: string;
  totalAmount: number;
  status: string;
  institutionName: string;
  maskedAccountNumber: string;
  accountHolder: string;
  inviteUrl: string;
};

function cookie(name: string): string | undefined {
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`))
    ?.split("=")
    .slice(1)
    .join("=");
}

async function csrfToken(): Promise<string> {
  let token = cookie("XSRF-TOKEN");
  if (!token) {
    const response = await fetch(`${API_URL}/api/csrf`, { credentials: "include" });
    if (!response.ok) throw new Error(response.status === 401 ? "LOGIN_REQUIRED" : "보안 토큰을 발급하지 못했습니다.");
    token = cookie("XSRF-TOKEN");
  }
  if (!token) throw new Error("보안 토큰을 확인하지 못했습니다.");
  return decodeURIComponent(token);
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const method = (init?.method ?? "GET").toUpperCase();
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json");
  if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    headers.set("X-XSRF-TOKEN", await csrfToken());
  }
  const response = await fetch(`${API_URL}${path}`, {
    credentials: "include",
    ...init,
    headers,
  });
  if (response.status === 401) throw new Error("LOGIN_REQUIRED");
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message ?? "요청에 실패했습니다.");
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}
export const oauthUrl = (provider: "kakao" | "google") =>
  `${API_URL}/oauth2/authorization/${provider}`;
