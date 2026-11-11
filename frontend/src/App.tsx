import { Suspense, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { Client } from "@stomp/stompjs";
import QRCode from "qrcode";
import {
  api,
  oauthUrl,
  WS_URL,
  type Account,
  type AuditLog,
  type History,
  type Room,
  type Snapshot,
} from "./api";
import "./App.css";
import "./flows.css";
import AccountPage from "./AccountPage";
import "./payments.css";
import "./completion.css";
import LogoutButton from "./LogoutButton";
import "./logout.css";
import "./participant-amount.css";
import "./history.css";
import "./payment-flow.css";
import { PageSkeleton } from "./components/Skeleton";

const won = (value: number) => `${value.toLocaleString("ko-KR")}원`;

async function showRoomNotification(title: string, body: string, roomId: string) {
  if (!("Notification" in window) || Notification.permission !== "granted") return;
  if ("serviceWorker" in navigator) {
    const registration = await navigator.serviceWorker.ready;
    await registration.showNotification(title, { body, data: { url: `/rooms/${roomId}` }, tag: `room-${roomId}` });
  } else {
    new Notification(title, { body });
  }
}

function urlBase64ToUint8Array(value: string) {
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, "+").replace(/_/g, "/");
  return Uint8Array.from(atob(base64), (character) => character.charCodeAt(0));
}

function Login() {
  const [params] = useSearchParams();
  const remember = () =>
    sessionStorage.setItem("oauthReturnTo", params.get("returnTo") || "/");
  return (
    <main className="app-shell login">
      <div className="login-brand">NBBANG</div>
      <h1>
        먹은 만큼만
        <br />
        깔끔하게 나눠요.
      </h1>
      <p>로그인하고 내 계좌와 정산 기록을 안전하게 관리하세요.</p>
      <div className="login-actions">
        <a className="kakao" href={oauthUrl("kakao")} onClick={remember}>
          카카오로 계속하기
        </a>
        <a className="google" href={oauthUrl("google")} onClick={remember}>
          Google로 계속하기
        </a>
      </div>
    </main>
  );
}

function Home() {
  const { data: history } = useSuspenseQuery({
    queryKey: ["room-history"],
    queryFn: () => api<History[]>("/api/rooms/history"),
  });
  const nav = useNavigate();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<"ALL" | "OPEN" | "COMPLETED">("ALL");
  const filteredHistory = useMemo(() => history.filter((item) =>
    item.title.toLocaleLowerCase("ko-KR").includes(query.trim().toLocaleLowerCase("ko-KR"))
      && (status === "ALL" || item.status === status),
  ), [history, query, status]);
  return (
    <Shell>
      <section className="welcome">
        <p>함께 먹은 자리의 정산</p>
        <h1>
          오늘 먹은 메뉴만큼
          <br />
          간편하게 나눠요.
        </h1>
      </section>
      <section className="quick-actions">
        <button className="primary-action" onClick={() => nav("/rooms/new")}>
          <span className="action-icon">＋</span>
          <span>
            <strong>새 정산 시작</strong>
            <small>결제 금액과 받을 계좌 선택</small>
          </span>
        </button>
        <button
          className="secondary-action"
          onClick={() => {
            const token = prompt("초대 코드 또는 링크를 입력해 주세요");
            if (token) nav(`/join/${token.split("/").pop()}`);
          }}
        >
          <span className="action-icon">⌁</span>
          <span>
            <strong>QR로 참여</strong>
            <small>정산방 초대 코드 입력</small>
          </span>
        </button>
      </section>
      <section className="history">
        <div className="section-heading">
          <h2>최근 정산</h2>
        </div>
        <div className="history-controls">
          <input type="search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="정산 이름 검색" aria-label="정산 이름 검색" />
          <select value={status} onChange={(event) => setStatus(event.target.value as typeof status)} aria-label="정산 상태">
            <option value="ALL">전체</option>
            <option value="OPEN">진행 중</option>
            <option value="COMPLETED">정산 완료</option>
          </select>
        </div>
        <div className="history-list">
          {filteredHistory.length === 0 ? (
            <p className="empty">{history.length === 0 ? "아직 정산 기록이 없어요." : "조건에 맞는 정산이 없어요."}</p>
          ) : (
            filteredHistory.map((h) => (
              <Link className="history-card" to={`/rooms/${h.id}`} key={h.id}>
                <div className="receipt">₩</div>
                <div className="history-copy">
                  <strong>{h.title}</strong>
                  <span>
                    {new Date(h.createdAt).toLocaleDateString("ko-KR")} ·{" "}
                    {h.role === "HOST" ? "방장" : "참가자"}
                  </span>
                </div>
                <div className="history-value">
                  <strong>{won(h.totalAmount)}</strong>
                  <span>
                    {h.status === "COMPLETED" ? "정산 완료" : h.status === "CANCELLED" ? "취소됨" : "진행 중"}
                  </span>
                </div>
              </Link>
            ))
          )}
        </div>
      </section>
    </Shell>
  );
}

function NewRoom() {
  const { data: accounts } = useSuspenseQuery({
    queryKey: ["accounts"],
    queryFn: () => api<Account[]>("/api/accounts"),
  });
  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState("");
  const [accountId, setAccountId] = useState(() => accounts.find((account) => account.primary)?.id || accounts[0]?.id || "");
  const [error, setError] = useState("");
  const nav = useNavigate();
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    try {
      const room = await api<Room>("/api/rooms", {
        method: "POST",
        body: JSON.stringify({ title, totalAmount: Number(amount), accountId }),
      });
      nav(`/rooms/${room.id}`, { state: room });
    } catch (e) {
      setError((e as Error).message);
    }
  }
  return (
    <Shell>
      <section className="form-page">
        <Link to="/">← 홈</Link>
        <h1>새 정산 시작</h1>
        <form onSubmit={submit}>
          <label>
            정산 이름
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 성수 저녁 모임"
              required
            />
          </label>
          <label>
            총 결제 금액
            <input
              inputMode="numeric"
              value={amount ? Number(amount).toLocaleString("ko-KR") : ""}
              onChange={(e) => setAmount(e.target.value.replace(/\D/g, ""))}
              placeholder="0"
              required
            />
          </label>
          <label>
            송금받을 계좌
            <select
              value={accountId}
              onChange={(e) => setAccountId(e.target.value)}
              required
            >
              <option value="">계좌를 선택하세요</option>
              {accounts.map((a) => (
                <option value={a.id} key={a.id}>
                  {a.alias || a.institutionName} · {a.maskedAccountNumber}
                </option>
              ))}
            </select>
          </label>
          {accounts.length === 0 && (
            <p className="hint">먼저 내 계좌를 등록해야 합니다.</p>
          )}
          {error && <p className="error">{error}</p>}
          <button className="submit" disabled={!accountId}>
            정산방 만들기
          </button>
        </form>
      </section>
    </Shell>
  );
}

function JoinRoom() {
  const { token } = useParams();
  const nav = useNavigate();
  const [error, setError] = useState("");
  useEffect(() => {
    if (token)
      api<Room>(`/api/rooms/join/${token}`, { method: "POST" })
        .then((r) => nav(`/rooms/${r.id}`, { replace: true, state: r }))
        .catch((e) =>
          e.message === "LOGIN_REQUIRED"
            ? nav(`/login?returnTo=${encodeURIComponent(`/join/${token}`)}`, {
                replace: true,
              })
            : setError(e.message),
        );
  }, [token, nav]);
  return (
    <Shell>
      <section className="center-state">
        <h2>정산방에 참여하는 중...</h2>
        {error && (
          <>
            <p className="error">{error}</p>
            <Link to="/">홈으로</Link>
          </>
        )}
      </section>
    </Shell>
  );
}

function RoomPage() {
  const { roomId } = useParams();
  const { data: initialSnapshot } = useSuspenseQuery({ queryKey: ["room", roomId], queryFn: () => api<Snapshot>(`/api/rooms/${roomId}`) });
  const { data: me } = useSuspenseQuery({ queryKey: ["me"], queryFn: () => api<{ id: string; name: string }>("/api/me") });
  const { data: auditLogs } = useSuspenseQuery({ queryKey: ["room-audit", roomId], queryFn: () => api<AuditLog[]>(`/api/rooms/${roomId}/audit`) });
  const queryClient = useQueryClient();
  const initialMine = initialSnapshot.participants.find((participant) => participant.userId === me.id);
  const [snap, setSnap] = useState<Snapshot>(initialSnapshot);
  const [amount, setAmount] = useState(() => String(initialMine?.amount || ""));
  const [error, setError] = useState("");
  const [pendingPaymentProvider, setPendingPaymentProvider] = useState<"toss" | "kakao" | null>(null);
  const [showPaymentReturn, setShowPaymentReturn] = useState(false);
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermission>(
    "Notification" in window ? Notification.permission : "denied",
  );
  const meRef = useRef<{ id: string; name: string }>(me);
  const snapshotRef = useRef<Snapshot | null>(initialSnapshot);
  const myAmountRef = useRef(initialMine?.amount ?? 0);
  const qr = useRef<HTMLCanvasElement>(null);
  const invite = snap?.inviteUrl ?? "";
  useEffect(() => { meRef.current = me; }, [me]);
  const acceptSnapshot = useCallback((next: Snapshot) => {
    const previous = snapshotRef.current;
    const currentUser = meRef.current;
    snapshotRef.current = next;
    setSnap(next);
    if (!previous || !currentUser || !roomId) return;
    const mine = next.participants.find((participant) => participant.userId === currentUser.id);
    if (mine && mine.amount !== myAmountRef.current) {
      myAmountRef.current = mine.amount;
      setAmount(String(mine.amount || ""));
    }
    if (mine?.role === "HOST") {
      for (const participant of next.participants.filter((item) => item.role === "MEMBER")) {
        const before = previous.participants.find((item) => item.participantId === participant.participantId);
        if (before?.paymentStatus !== "SENT" && participant.paymentStatus === "SENT") {
          void showRoomNotification("송금 확인 요청", `${participant.userName}님이 송금했다고 알려왔어요.`, roomId);
        }
      }
    } else {
      const beforeMine = previous.participants.find((participant) => participant.userId === currentUser.id);
      if (beforeMine?.paymentStatus !== "CONFIRMED" && mine?.paymentStatus === "CONFIRMED") {
        void showRoomNotification("정산 확인 완료", "방장이 송금을 확인했어요.", roomId);
      }
    }
  }, [roomId]);
  const roomMutation = useMutation({
    mutationFn: ({ path, init }: { path: string; init: RequestInit }) => api<Snapshot>(path, init),
    onSuccess: (next) => {
      acceptSnapshot(next);
      queryClient.setQueryData(["room", roomId], next);
      void queryClient.invalidateQueries({ queryKey: ["room-audit", roomId] });
      void queryClient.invalidateQueries({ queryKey: ["room-history"] });
    },
  });
  useEffect(() => {
    if (!roomId) return;
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 3000,
      onConnect: () =>
        client.subscribe(`/topic/rooms/${roomId}`, (m) =>
          acceptSnapshot(JSON.parse(m.body)),
        ),
    });
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [roomId, acceptSnapshot]);
  useEffect(() => {
    let leftForPayment = false;
    const onVisibility = () => {
      if (document.visibilityState === "hidden") leftForPayment = true;
      if (document.visibilityState === "visible" && leftForPayment && sessionStorage.getItem("paymentReturnRoomId") === roomId) {
        setShowPaymentReturn(true);
        leftForPayment = false;
      }
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => document.removeEventListener("visibilitychange", onVisibility);
  }, [roomId]);
  useEffect(() => {
    if (qr.current && invite)
      QRCode.toCanvas(qr.current, invite, { width: 180, margin: 1 });
  }, [invite]);
  async function saveAmount(e: React.FormEvent) {
    e.preventDefault();
    try {
      await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/amount`, init: {
          method: "PUT",
          body: JSON.stringify({ amount: Number(amount) }),
        } });
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function copyAccount() {
    if (!snap) return;
    await navigator.clipboard.writeText(
      `${snap.recipient.institutionName} ${snap.recipient.accountNumber}`,
    );
  }
  function launchPayment(provider: "toss" | "kakao") {
    if (!snap || !myTotal) return;
    setPendingPaymentProvider(null);
    void copyAccount();
    const r = snap.recipient;
    sessionStorage.setItem("paymentReturnRoomId", roomId ?? "");
    const url =
      provider === "toss"
        ? `supertoss://send?amount=${myTotal.amount}&bank=${encodeURIComponent(r.institutionName)}&accountNo=${r.accountNumber}`
        : `https://link.kakaopay.com/t/money/to/bank?amount=${myTotal.amount}&bank_code=${r.institutionCode}&bank_account_number=${r.accountNumber}`;
    if (/Android|iPhone|iPad|iPod/i.test(navigator.userAgent))
      window.location.href = url;
    else
      window.open(
        provider === "toss" ? "https://toss.me" : url,
        "_blank",
        "noopener,noreferrer",
      );
  }
  async function markSent() {
    await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/payments/sent`, init: {
        method: "POST",
      } });
    sessionStorage.removeItem("paymentReturnRoomId");
    setShowPaymentReturn(false);
  }
  async function confirmPayment(participantId: string) {
    await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/payments/${participantId}/confirm`, init: { method: "POST" } });
  }
  async function completeRoom() {
    try {
      await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/complete`, init: { method: "POST" } });
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function completeAll() {
    if (!window.confirm("모든 참가자를 입금 확인 처리하고 정산을 완료할까요?")) return;
    try {
      await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/complete-all`, init: { method: "POST" } });
    } catch (e) {
      setError((e as Error).message);
    }
  }
  async function cancelRoom() {
    if (!window.confirm("이 정산방을 취소할까요? 취소 후에는 금액과 송금 상태를 바꿀 수 없습니다.")) return;
    try {
      await roomMutation.mutateAsync({ path: `/api/rooms/${roomId}/cancel`, init: { method: "POST" } });
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function enableNotifications() {
    try {
      if (!("serviceWorker" in navigator) || !("PushManager" in window))
        throw new Error("이 브라우저는 푸시 알림을 지원하지 않습니다.");
      const permission = await Notification.requestPermission();
      setNotificationPermission(permission);
      if (permission !== "granted") return;
      const { publicKey } = await api<{ publicKey: string }>("/api/push/public-key");
      if (!publicKey) throw new Error("서버의 푸시 알림 키가 아직 설정되지 않았습니다.");
      const registration = await navigator.serviceWorker.ready;
      const existing = await registration.pushManager.getSubscription();
      const subscription = existing ?? await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey),
      });
      await api<void>("/api/push/subscriptions", { method: "POST", body: JSON.stringify(subscription.toJSON()) });
    } catch (e) {
      setError((e as Error).message);
    }
  }
  const grouped = useMemo(() => snap?.participants ?? [], [snap]);
  const myTotal = grouped.find((p) => p.userId === me?.id);
  const isHost = myTotal?.role === "HOST";
  return (
    <Shell>
      <section className="room-page">
        <Link to="/">← 홈</Link>
        {notificationPermission !== "granted" && (
          <button className="notification-opt-in" type="button" onClick={enableNotifications}>알림 받기</button>
        )}
        <div className="amount-summary">
          <span>남은 미정산액</span>
          <strong>{won(snap?.remainingAmount ?? 0)}</strong>
          <small>
            총 {won(snap?.totalAmount ?? 0)} 중{" "}
            {won(snap?.allocatedAmount ?? 0)} 입력
          </small>
        </div>
        <details className="qr-card">
          <summary>친구 초대 QR 보기</summary>
          <canvas ref={qr} />
          <button onClick={() => navigator.clipboard.writeText(invite)}>
            초대 링크 복사
          </button>
        </details>
        <form className="amount-form" onSubmit={saveAmount}>
          <label>내가 먹은 금액</label>
          <input
            inputMode="numeric"
            value={amount ? Number(amount).toLocaleString("ko-KR") : ""}
            onChange={(e) => setAmount(e.target.value.replace(/\D/g, ""))}
            placeholder="0"
            required
            disabled={snap?.status !== "OPEN"}
          />
          <span>원</span>
          <button disabled={snap?.status !== "OPEN"}>금액 저장</button>
        </form>
        {error && <p className="error">{error}</p>}
        <section className="totals">
          <h2>참가자별 금액</h2>
          {grouped.map((p) => (
            <div className={p.userId === me?.id ? "mine" : ""} key={p.userId}>
              <span>{p.userName}{p.userId === me?.id ? " (나)" : ""}</span>
              <strong>{won(p.amount)}</strong>
            </div>
          ))}
        </section>
        <details className="audit-history">
          <summary>변경 이력 {auditLogs.length}건</summary>
          <div>
            {auditLogs.length === 0 ? <p className="empty">아직 변경 이력이 없어요.</p> : auditLogs.map((log, index) => (
              <article key={`${log.createdAt}-${index}`}>
                <strong>{auditLabel(log.eventType)}</strong>
                <span>{log.actorName} · {new Date(log.createdAt).toLocaleString("ko-KR")}</span>
                {typeof log.afterData?.amount === "number" && <small>{won(log.afterData.amount)}</small>}
              </article>
            ))}
          </div>
        </details>
        {snap?.status === "OPEN" && myTotal && !isHost && (
          <section className="payment-card">
            <h2>방장에게 {won(myTotal.amount)} 송금하기</h2>
            <p>
              {snap.recipient.institutionName} {snap.recipient.accountNumber} ·{" "}
              {snap.recipient.accountHolder}
            </p>
            <button className="toss-pay" onClick={() => setPendingPaymentProvider("toss")}>
              토스로 송금하기
            </button>
            <button className="kakao-pay" onClick={() => setPendingPaymentProvider("kakao")}>
              카카오페이로 송금하기
            </button>
            <button
              className="sent"
              disabled={myTotal.paymentStatus === "CONFIRMED"}
              onClick={markSent}
            >
              {myTotal.paymentStatus === "CONFIRMED"
                ? "방장이 확인했어요"
                : myTotal.paymentStatus === "SENT"
                  ? "송금 확인 대기 중"
                  : "송금했어요"}
            </button>
          </section>
        )}
        {isHost && (
          <section className="payment-card">
            <h2>송금 현황</h2>
            {grouped
              .filter((p) => p.role === "MEMBER")
              .map((p) => (
                <div className="payment-row" key={p.participantId}>
                  <span>
                    {p.userName} · {won(p.amount)}
                  </span>
                  {p.paymentStatus === "CONFIRMED" ? (
                    <b>확인 완료</b>
                  ) : (
                    <button
                      disabled={snap?.status !== "OPEN" || p.paymentStatus !== "SENT"}
                      onClick={() => confirmPayment(p.participantId)}
                    >
                      {p.paymentStatus === "SENT" ? "입금 확인" : "송금 전"}
                    </button>
                  )}
                </div>
              ))}
          </section>
        )}
        {isHost && (
          <div className="completion-actions">
            <button className="complete-room secondary-complete" disabled={snap?.status !== "OPEN"} onClick={completeRoom}>
              {snap?.status === "COMPLETED" ? "정산 완료됨" : snap?.status === "CANCELLED" ? "취소된 정산" : "확인된 송금으로 완료"}
            </button>
            <button className="complete-room" disabled={snap?.status !== "OPEN"} onClick={completeAll}>
              일괄 정산 완료
            </button>
            <button className="danger" disabled={snap?.status !== "OPEN"} onClick={cancelRoom}>
              정산방 취소
            </button>
          </div>
        )}
        {showPaymentReturn && (
          <div className="modal-backdrop" role="presentation">
            <section className="payment-return-modal" role="dialog" aria-modal="true" aria-labelledby="payment-return-title">
              <h2 id="payment-return-title">송금을 완료하셨나요?</h2>
              <p>실제 송금을 마쳤다면 방장에게 확인 요청을 보낼게요.</p>
              <button className="confirm-sent" onClick={markSent}>네, 송금했어요</button>
              <button className="cancel-return" onClick={() => setShowPaymentReturn(false)}>아니요, 아직이에요</button>
            </section>
          </div>
        )}
        {pendingPaymentProvider && (
          <div className="modal-backdrop" role="presentation">
            <section className="payment-return-modal" role="dialog" aria-modal="true" aria-labelledby="payment-guide-title">
              <h2 id="payment-guide-title">송금 앱으로 이동할게요</h2>
              <p>
                송금을 마친 뒤 아이폰 왼쪽 위의 ‘NBBANG으로 돌아가기’를 누르거나 앱 전환 화면에서 NBBANG으로 돌아와 주세요.
                돌아오면 송금 여부를 확인합니다.
              </p>
              <button className="confirm-sent" onClick={() => launchPayment(pendingPaymentProvider)}>
                {pendingPaymentProvider === "toss" ? "토스" : "카카오페이"} 열기
              </button>
              <button className="cancel-return" onClick={() => setPendingPaymentProvider(null)}>취소</button>
            </section>
          </div>
        )}
      </section>
    </Shell>
  );
}

function auditLabel(eventType: string) {
  return ({ PARTICIPANT_AMOUNT_UPDATED: "먹은 금액 변경", PAYMENT_SENT: "송금 완료 알림", PAYMENT_CONFIRMED: "입금 확인", ROOM_COMPLETED: "정산 완료", ROOM_COMPLETED_ALL: "일괄 정산 완료", ROOM_CANCELLED: "정산방 취소" } as Record<string, string>)[eventType] ?? eventType;
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <main className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/">
          NBBANG
        </Link>
        <Link className="profile" to="/accounts">N</Link>
      </header>
      {children}
    </main>
  );
}
function RequireAuth() {
  const location = useLocation();
  const auth = useQuery({ queryKey: ["me"], queryFn: () => api("/api/me"), retry: false });
  if (auth.isPending) {
    return <main className="app-shell center-state"><p>로그인 확인 중...</p></main>;
  }
  if (auth.isError) {
    const returnTo = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?returnTo=${encodeURIComponent(returnTo)}`} replace />;
  }
  return <Suspense fallback={<PageSkeleton />}><Outlet /><div className="global-logout"><LogoutButton /></div></Suspense>;
}
function OAuthCallback() {
  const navigate = useNavigate();
  useEffect(() => {
    const target = sessionStorage.getItem("oauthReturnTo") || "/";
    sessionStorage.removeItem("oauthReturnTo");
    navigate(target, { replace: true });
  }, [navigate]);
  return (
    <main className="app-shell center-state">
      <h2>로그인을 완료하는 중...</h2>
    </main>
  );
}
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/auth/callback" element={<OAuthCallback />} />
        <Route element={<RequireAuth />}>
          <Route path="/" element={<Home />} />
          <Route path="/rooms/new" element={<NewRoom />} />
          <Route path="/rooms/:roomId" element={<RoomPage />} />
          <Route path="/join/:token" element={<JoinRoom />} />
          <Route path="/accounts" element={<AccountPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
