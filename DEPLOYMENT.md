# NBBANG 운영 배포

## 1. Cloudflare DNS

Cloudflare 대시보드에서 도메인을 선택한 뒤 **DNS → Records → Add record**에서 아래 A 레코드를 추가한다.

| Type | Name | IPv4 address | Proxy |
|---|---|---|---|
| A | `nbbang` | 서버 공인 IP | Proxied |
| A | `assets.nbbang` | 서버 공인 IP | Proxied |
| A | `monitor.nbbang` | 서버 공인 IP | Proxied |
| A | `api.nbbang` | 서버 공인 IP | Proxied (이전 주소 리다이렉트용) |

**SSL/TLS → Overview**는 `Full (strict)`로 설정한다. Cloudflare 프록시를 켜도 원본 서버의 80/443 포트는 열려 있어야 Caddy가 인증서를 발급하고 요청을 받을 수 있다.

OAuth 콘솔의 운영 Redirect URI는 같은 오리진 주소로 변경한다.

- Kakao: `https://nbbang.minngyuseong.com/login/oauth2/code/kakao`
- Google: `https://nbbang.minngyuseong.com/login/oauth2/code/google`

## 2. 클라우드와 서버 방화벽

Oracle Cloud 기준으로 **Networking → Virtual Cloud Networks → 사용 중인 VCN → Security Lists 또는 Network Security Groups → Ingress Rules**에서 다음을 허용한다.

- TCP 80, source `0.0.0.0/0`
- TCP 443, source `0.0.0.0/0`
- UDP 443, source `0.0.0.0/0` (HTTP/3, 선택)
- TCP 22, source는 가능하면 관리자 본인 IP `/32`만

서버가 Ubuntu이고 UFW를 사용한다면 다음을 실행한다.

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
sudo ufw status
```

PostgreSQL 5432, 백엔드 8080, Grafana 3000, Prometheus 9090은 외부에 열지 않는다. Compose 내부 네트워크로만 통신한다.

## 3. 서버 환경 변수

서버의 프로젝트 폴더에서 `.env.example`을 `.env`로 복사하고 `.env`만 수정한다. `.env`는 Git에 올리지 않는다.

```bash
cp .env.example .env
openssl rand -base64 36        # POSTGRES_PASSWORD와 Grafana 비밀번호 생성용
openssl rand -base64 32        # ACCOUNT_ENCRYPTION_KEY 생성용
```

현재 개발 PC의 `.env`에는 `POSTGRES_PASSWORD=change-me`가 남아 있으므로 반드시 교체해야 한다. `GRAFANA_ADMIN_PASSWORD`도 새로 설정한다. `ACCOUNT_ENCRYPTION_KEY`는 배포 후 변경하면 기존 계좌번호를 복호화할 수 없으므로 비밀 저장소에 별도 보관한다.

## 4. 배포와 확인

서버에 Docker Engine과 Compose plugin을 설치한 뒤 실행한다.

```bash
bash ops/deploy.sh
docker compose -f compose.prod.yaml logs --tail=200 backend caddy
curl -fsS https://nbbang.minngyuseong.com/healthz
```

서비스 주소:

- 앱: `https://nbbang.minngyuseong.com`
- 모니터링: `https://monitor.nbbang.minngyuseong.com`
- 정적 금융기관 이미지: `https://assets.nbbang.minngyuseong.com`

Grafana의 `NBBANG / NBBANG Overview` 대시보드에서 백엔드 상태, 요청량, 5xx, JVM heap, DB connection pool을 확인한다. Cloudflare Access를 사용한다면 `monitor.nbbang.minngyuseong.com`을 본인 이메일만 허용하는 Self-hosted application으로 등록한다.

## 5. 백업과 복구

`postgres-backup` 컨테이너가 매일 `backups/`에 custom-format dump를 만들고 기본 14일간 보관한다. 서버 장애에도 살아남도록 이 폴더를 Cloudflare R2 또는 별도 스토리지에 추가 복제해야 한다.

복구 예시:

```bash
docker compose -f compose.prod.yaml exec -T postgres pg_restore \
  --clean --if-exists --no-owner --dbname="$POSTGRES_DB" \
  < backups/nbbang-YYYYMMDDTHHMMSSZ.dump
```

복구 명령은 기존 데이터를 변경하므로 실제 실행 전 최신 백업 파일과 대상 DB를 반드시 재확인한다.
