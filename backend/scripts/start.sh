#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

export MC_LISTEN_ADDR="${MC_LISTEN_ADDR:-127.0.0.1:8080}"
: "${MC_DATABASE_URL:?Set MC_DATABASE_URL before starting MultiChoice Backend}"

PGDATA="$PREFIX/var/lib/postgresql"
if ! pg_ctl -D "$PGDATA" status >/dev/null 2>&1; then
  pg_ctl -D "$PGDATA" -l "$PGDATA/server.log" start
fi

cd "$HOME/services/multichoice/backend"
if [ ! -x ./multichoice-api ]; then
  go build -o multichoice-api ./cmd/api
fi
exec ./multichoice-api
