# NBBANG

먹은 금액을 참가자별로 입력하고 실시간으로 송금 상태를 공유하는 모바일 정산 PWA입니다.

운영 배포, Cloudflare DNS, 방화벽, 환경 변수, 모니터링과 백업 절차는 [DEPLOYMENT.md](DEPLOYMENT.md)를 따릅니다.

QR로 정산방에 참여하고 참가자별 금액을 실시간으로 나누는 모바일 우선 PWA입니다.

## 서비스 주소

- Web: `https://nbbang.minngyuseong.com`
- API: `https://api.nbbang.minngyuseong.com`

## 구성

- `frontend`: React, TypeScript, Vite, PWA
- `backend`: Spring Boot, Spring Security OAuth2, WebSocket, JPA, Flyway
- Database: PostgreSQL

## 로컬 실행

1. `.env.example`을 `.env`로 복사하고 값을 설정합니다.
2. `docker compose -f compose.local.yaml up -d`로 PostgreSQL을 실행합니다.
3. `backend`에서 `./gradlew bootRun`을 실행합니다.
4. `frontend`에서 `pnpm install`, `pnpm dev`를 실행합니다.

전체 로컬 컨테이너는 `docker compose -f compose.local.yaml up -d --build`로 실행합니다. 웹은 `http://localhost:5173`, API 상태는 `http://localhost:8080/actuator/health`에서 확인합니다.

## Oracle 서버 배포

1. `nbbang`, `api.nbbang`, `assets.nbbang` 서브도메인의 A 레코드를 Oracle 서버 공인 IP로 지정합니다.
2. Oracle Cloud 보안 목록과 서버 방화벽에서 TCP 80/443, UDP 443을 허용합니다.
3. `.env.example`을 `.env`로 복사하고 운영 값을 입력합니다.
4. `ACCOUNT_ENCRYPTION_KEY`는 `openssl rand -base64 32`로 한 번 생성해 안전하게 보관합니다. 키를 잃으면 저장된 계좌번호를 복구할 수 없습니다.
5. 카카오와 Google 콘솔에 `https://api.nbbang.minngyuseong.com/login/oauth2/code/kakao`, `https://api.nbbang.minngyuseong.com/login/oauth2/code/google`을 콜백 URL로 등록합니다.
6. `docker compose -f compose.prod.yaml up -d --build`로 실행합니다.

Caddy가 세 도메인의 HTTPS 인증서와 WebSocket 프록시를 자동 관리합니다. Cloudflare 프록시를 사용한다면 SSL/TLS 모드는 `Full (strict)`로 설정합니다.

금융기관 SVG는 `assets/financial-institutions`에 포함되어 있고 운영에서는 `assets.nbbang.minngyuseong.com`으로 제공됩니다. 원본과 라이선스 정보는 해당 디렉터리의 `SOURCE.md`와 `LICENSE-korea-bank-icons.txt`를 확인하세요.
