# MultiChoice Medical — Commercial Platform Architecture

## Product decision
Two separately installable Android applications share one independently hosted backend:
- Client: `com.multichoice.medical` — learner experience.
- Admin: `com.multichoice.admin` — privileged content and business operations.
- Backend: Go + Fiber API and PostgreSQL, running directly on the dedicated Android phone through Termux. Docker is not part of this deployment.

The two apps are separate trust boundaries. Admin authorization is enforced by the backend, never by hiding screens in an APK.

## Runtime topology
- Android phone: Termux, PostgreSQL, Go API service.
- API: Fiber HTTP server; local development bind defaults to `127.0.0.1:8080`.
- Database: PostgreSQL bound to loopback on port 5432.
- Android apps: call the API over the local network or a secured remote connection after authentication and TLS are implemented.
- CI: GitHub Actions compiles/tests the Go backend and builds the two Android apps.

The phone is a development/private pilot host, not a guaranteed 24/7 commercial server. Android battery management, process reclamation, reboots, mobile-network changes and Wi-Fi availability can interrupt it. Production launch requires a resilient hosting and backup strategy.

## Repository layout
- `app/`: existing learner Android application; package identity is retained.
- `adminApp/`: independent administrator Android application.
- `backend/`: Go/Fiber API, PostgreSQL migration and Termux scripts.
- `supabase/migrations/`: earlier Supabase schema draft; not used by the new phone-hosted backend.
- `.github/workflows/android-build.yml`: independent Android APK builds.
- `.github/workflows/backend-build.yml`: Go test/build workflow.

## Backend architecture
1. HTTP/API: Go 1.23+, Fiber v2, versioned `/api/v1` routes.
2. Database: PostgreSQL, accessed through pgx connection pool.
3. Identity: backend-managed accounts, password hashing, short-lived access tokens and refresh-token rotation (next implementation phase).
4. Authorization: server-side role checks for `student`, `content_editor`, `admin`, `owner`; all privileged mutations audited.
5. Content: subjects, questions, options, protected answer keys, exams and exam composition.
6. Commercial: subscription entitlements updated only by verified payment callbacks; never trust client-submitted subscription state.
7. Media: private object storage or an authenticated media service; do not expose arbitrary phone filesystem paths.
8. Observability: structured logs, health/readiness endpoints, backups and database migration tracking.

## Question security
Correct answers must never be returned from learner-facing question endpoints. Answer keys stay in a separate table and grading happens inside trusted backend logic. Do not ship database credentials, JWT signing secrets or administrator passwords in either APK or in Git.

## Client application modules
- onboarding, sign-in, password recovery and profile
- home dashboard and subject catalogue
- searchable/filterable question bank
- timed and untimed exams
- answer explanations and references
- bookmarks, incorrect-answer review and revision
- attempt history, analytics and progress goals
- subscription plans and notifications
- offline cache with explicit sync state

## Admin application modules
- secure sign-in and server-side role verification
- operational dashboard and audit log
- subjects, categories, tags and question CRUD
- rich question editor: text, images, tables, options, correct answer, explanation, references and difficulty
- bulk import/export with validation and duplicate detection
- draft/review/published/archived content lifecycle
- user and role management
- subscription/entitlement oversight
- exam templates, analytics, announcements and notification campaigns
- content versioning and restore

## Delivery phases
- Phase 1 — Foundation: separate Android modules, Go backend skeleton, PostgreSQL schema, Termux setup and CI.
- Phase 2 — Identity: registration/login, password hashing, token lifecycle, role provisioning and authorization tests.
- Phase 3 — Content operations: admin question editor, taxonomy, media handling, review/publish workflow and import/export.
- Phase 4 — Learner product: catalogue, exam engine, bookmarks, revision, analytics and offline sync.
- Phase 5 — Commercial: subscription catalogue, payment provider integration/webhooks, entitlements and notifications.
- Phase 6 — Production: security review, automated tests, monitoring, backups, TLS, signed release builds and reliable hosting.

## Current implementation status
The admin Android module has an initial Arabic RTL navigation/dashboard shell. The Go backend currently provides health/readiness and API version endpoints and checks PostgreSQL connectivity. A local PostgreSQL schema and Termux scripts are committed. Authentication, content CRUD, grading, subscriptions, TLS and remote access are not implemented yet. The schema must be applied and tested before use.
