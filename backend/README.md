# MultiChoice Backend (Android / Termux)

A standalone Go + Fiber API with PostgreSQL, intended to run directly on the dedicated Android phone through Termux. Docker is intentionally not used.

## Runtime defaults
- HTTP bind: 127.0.0.1:8080 (local-only safe default)
- PostgreSQL: 127.0.0.1:5432
- Health: GET /health
- Readiness: GET /ready
- API root: GET /api/v1

Do not bind to 0.0.0.0 or expose the phone to the public internet until authentication, TLS, rate limits, firewalling and hardened credentials are implemented.

## Termux setup

Install Termux from its official F-Droid or GitHub distribution, then:

```sh
pkg update && pkg upgrade
pkg install golang postgresql git
mkdir -p ~/services/multichoice
```

Initialize PostgreSQL once:

```sh
initdb "$PREFIX/var/lib/postgresql"
pg_ctl -D "$PREFIX/var/lib/postgresql" -l "$PREFIX/var/lib/postgresql/server.log" start
```

Create the application database and role (run the commands in the Termux PostgreSQL environment):

```sh
createuser multichoice
createdb -O multichoice multichoice
psql -d multichoice -c "ALTER USER multichoice WITH PASSWORD 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';"
```

Apply the schema from the repository root:

```sh
psql "postgres://multichoice:REPLACE_WITH_A_LONG_RANDOM_PASSWORD@127.0.0.1:5432/multichoice?sslmode=disable" -f backend/migrations/001_core.sql
```

Build and run:

```sh
cd backend
go mod download
go build -o multichoice-api ./cmd/api
export MC_DATABASE_URL='postgres://multichoice:REPLACE_WITH_A_LONG_RANDOM_PASSWORD@127.0.0.1:5432/multichoice?sslmode=disable'
export MC_LISTEN_ADDR='127.0.0.1:8080'
./multichoice-api
```

In another Termux session, verify:

```sh
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/ready
```

## Important operating notes
- The initial API only implements health/readiness and version discovery. Authentication, admin endpoints, question CRUD, exam grading and billing are not implemented yet.
- Keep the bind address at 127.0.0.1 during development. LAN access should be enabled only after adding authentication and TLS/reverse-proxy protections.
- Android battery optimization, process reclamation, device restarts and Wi-Fi changes can interrupt service. Termux:Boot can start PostgreSQL and the API after reboot, but cannot guarantee 24/7 uptime.
- Never embed database passwords or administrator credentials in APKs or commit them to Git.
- Backups to a second device or remote storage are required before production use.
