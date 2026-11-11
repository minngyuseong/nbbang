#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env and fill every secret first." >&2
  exit 1
fi

if grep -Eq '=(change-me|change-me-too|local-placeholder)?$' .env; then
  echo "Unsafe or empty values remain in .env." >&2
  exit 1
fi

docker compose -f compose.prod.yaml config --quiet
docker compose -f compose.prod.yaml up -d --build --remove-orphans
docker compose -f compose.prod.yaml ps

echo "Waiting for the public health endpoint..."
for attempt in {1..30}; do
  if curl --fail --silent https://nbbang.minngyuseong.com/healthz >/dev/null; then
    echo "NBBANG deployment is reachable."
    exit 0
  fi
  sleep 5
done

echo "Deployment started, but the public endpoint is not healthy yet." >&2
docker compose -f compose.prod.yaml logs --tail=200 backend caddy >&2
exit 1
