import { useMemo, useState } from "react";
import { useMutation, useQueryClient, useSuspenseQuery } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router-dom";
import { api, type Account, type Institution } from "./api";
import "./accounts.css";
import "./account-actions.css";

export default function AccountPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { data: accounts } = useSuspenseQuery({ queryKey: ["accounts"], queryFn: () => api<Account[]>("/api/accounts") });
  const { data: institutions } = useSuspenseQuery({ queryKey: ["institutions"], queryFn: () => api<Institution[]>("/api/institutions"), staleTime: Infinity });
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<"BANK" | "SECURITIES">("BANK");
  const [selected, setSelected] = useState<Institution>();
  const [editingId, setEditingId] = useState<string>();
  const [number, setNumber] = useState("");
  const [holder, setHolder] = useState("");
  const [alias, setAlias] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const accountMutation = useMutation({
    mutationFn: ({ path, init }: { path: string; init: RequestInit }) => api<unknown>(path, init),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["accounts"] }),
  });

  const visible = useMemo(
    () => institutions.filter((institution) => institution.category === category && institution.name.includes(query.trim())),
    [institutions, category, query],
  );

  function resetForm() {
    setEditingId(undefined);
    setSelected(undefined);
    setNumber("");
    setHolder("");
    setAlias("");
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!selected) return;
    setError("");
    const body = JSON.stringify({
      institutionCode: selected.code,
      institutionName: selected.name,
      accountNumber: number,
      accountHolder: holder,
      alias,
      primary: accounts.length === 0,
    });
    try {
      await accountMutation.mutateAsync({ path: editingId ? `/api/accounts/${editingId}` : "/api/accounts", init: { method: editingId ? "PUT" : "POST", body } });
      setNotice(editingId ? "계좌를 수정했습니다." : "계좌를 등록했습니다.");
      resetForm();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function startEdit(account: Account) {
    setError("");
    try {
      const revealed = await api<{ accountNumber: string }>(`/api/accounts/${account.id}/number`);
      const institution = institutions.find((item) => item.code === account.institutionCode);
      setEditingId(account.id);
      setSelected(institution);
      setCategory(institution?.category ?? "BANK");
      setNumber(revealed.accountNumber);
      setHolder(account.accountHolder);
      setAlias(account.alias ?? "");
      window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function copyNumber(account: Account) {
    try {
      const revealed = await api<{ accountNumber: string }>(`/api/accounts/${account.id}/number`);
      await navigator.clipboard.writeText(revealed.accountNumber);
      setNotice("계좌번호를 복사했습니다.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function makePrimary(id: string) {
    try {
      await accountMutation.mutateAsync({ path: `/api/accounts/${id}/primary`, init: { method: "PUT" } });
      setNotice("대표 계좌를 변경했습니다.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function remove(account: Account) {
    if (!window.confirm(`${account.alias || account.institutionName} 계좌를 삭제할까요?\n기존 정산 기록은 유지됩니다.`)) return;
    try {
      await accountMutation.mutateAsync({ path: `/api/accounts/${account.id}`, init: { method: "DELETE" } });
      if (editingId === account.id) resetForm();
      setNotice("계좌를 삭제했습니다.");
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function withdraw() {
    const confirmation = window.prompt("회원 탈퇴를 진행하려면 '탈퇴'를 입력해 주세요.");
    if (confirmation !== "탈퇴") return;
    try {
      await api<void>("/api/me", { method: "DELETE" });
      queryClient.clear();
      navigate("/login", { replace: true });
    } catch (e) {
      setError((e as Error).message);
    }
  }

  const minLength = selected?.category === "BANK" ? 10 : 8;
  const maxLength = selected?.category === "BANK" ? 16 : 20;

  return (
    <main className="app-shell account-page">
      <header className="topbar"><Link className="brand" to="/">NBBANG</Link><Link to="/">닫기</Link></header>
      <h1>내 계좌</h1>
      {notice && <p className="notice">{notice}</p>}
      <div className="account-list">
        {accounts.map((account) => (
          <article key={account.id}>
            <div className="institution-fallback">{account.institutionName[0]}</div>
            <span><strong>{account.alias || account.institutionName}</strong><small>{account.institutionName} · {account.maskedAccountNumber}</small><small>예금주 {account.accountHolder}</small></span>
            <div className="account-actions">
              {account.primary ? <b>대표</b> : <button onClick={() => makePrimary(account.id)}>대표로</button>}
              <button onClick={() => copyNumber(account)}>복사</button>
              <button onClick={() => startEdit(account)}>수정</button>
              <button className="danger" onClick={() => remove(account)}>삭제</button>
            </div>
          </article>
        ))}
        {accounts.length === 0 && <p className="empty">등록한 계좌가 없습니다.</p>}
      </div>

      <section className="register-account">
        <h2>{editingId ? "계좌 수정" : "계좌 추가"}</h2>
        <input className="search" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="은행·증권사 검색" />
        <div className="tabs">
          <button type="button" className={category === "BANK" ? "active" : ""} onClick={() => setCategory("BANK")}>은행</button>
          <button type="button" className={category === "SECURITIES" ? "active" : ""} onClick={() => setCategory("SECURITIES")}>증권사</button>
        </div>
        <div className="institution-grid">
          {visible.map((institution) => (
            <button type="button" className={selected?.code === institution.code ? "selected" : ""} onClick={() => setSelected(institution)} key={institution.code}>
              <span><img src={institution.logoUrl} onError={(e) => { e.currentTarget.style.display = "none"; }} alt="" /><i>{institution.name[0]}</i></span>
              <small>{institution.name}</small>
            </button>
          ))}
        </div>
        {selected && (
          <form onSubmit={submit}>
            <h3>{selected.name} 계좌 {editingId ? "수정" : "등록"}</h3>
            <input inputMode="numeric" minLength={minLength} maxLength={maxLength} value={number} onChange={(e) => setNumber(e.target.value.replace(/\D/g, ""))} placeholder={`계좌번호 ${minLength}~${maxLength}자리`} required />
            <input maxLength={100} value={holder} onChange={(e) => setHolder(e.target.value)} placeholder="예금주" required />
            <input maxLength={100} value={alias} onChange={(e) => setAlias(e.target.value)} placeholder="계좌 별칭 (선택)" />
            <button className="submit" disabled={accountMutation.isPending}>{accountMutation.isPending ? "저장 중..." : editingId ? "수정 저장" : "계좌 등록"}</button>
            {editingId && <button className="cancel-edit" type="button" onClick={resetForm}>수정 취소</button>}
          </form>
        )}
        {error && <p className="error">{error}</p>}
      </section>
      <section className="register-account">
        <h2>계정 관리</h2>
        <p>탈퇴하면 등록 계좌와 푸시 구독을 삭제하고 완료된 정산 기록의 사용자 정보는 익명화합니다.</p>
        <button type="button" className="danger" onClick={withdraw}>회원 탈퇴</button>
      </section>
    </main>
  );
}
