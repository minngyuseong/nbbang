import { useNavigate } from "react-router-dom";
import { api } from "./api";

export default function LogoutButton() {
  const navigate = useNavigate();

  async function logout() {
    try {
      await api<void>("/api/logout", { method: "POST" });
    } finally {
      navigate("/login", { replace: true });
    }
  }

  return <button className="logout-button" type="button" onClick={logout}>로그아웃</button>;
}
