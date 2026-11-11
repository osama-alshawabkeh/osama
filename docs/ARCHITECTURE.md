# MultiChoice Medical — Commercial Platform Architecture

## Product decision
Two separately installable Android applications share one backend:
- Client: com.multichoice.medical — learner experience.
- Admin: com.multichoice.admin — privileged content and business operations.

The applications are separate trust boundaries, not merely two navigation modes in one APK. Admin authorization must be enforced by PostgreSQL RLS and server-side functions; hiding admin screens in a client is never authorization.

## Repository layout
- app/: existing learner application; its application ID remains stable for upgrades.
- adminApp/: independent administrator application.
- supabase/migrations/: versioned database migrations.
- .github/workflows/android-build.yml: independent client/admin build and artifact jobs.

## Target architecture
1. Android presentation: Kotlin, Jetpack Compose, Material 3, Arabic RTL, adaptive layouts.
2. App architecture: feature-oriented UI, ViewModels, repositories, immutable UI state, coroutines/Flow.
3. Local data: Room cache for downloaded learner content and offline exam sessions. Never cache privileged answer keys in a public client data set.
4. Backend: Supabase Auth, PostgreSQL, Storage, Edge Functions.
5. Authorization: profile roles (student, content_editor, admin, owner), RLS on every exposed table, server-side role checks, audit events for privileged mutations.
6. Notifications: Firebase Cloud Messaging, with device tokens tied to authenticated users.
7. Commercial services: subscription entitlements stored server-side; payment provider webhooks update entitlements. Never trust a client-supplied subscription state.
8. Delivery: GitHub Actions builds two independent debug APK artifacts. Release signing uses protected GitHub environment secrets and is a separate future workflow.

## Core product modules

### Client
- onboarding, sign-in, password recovery and profile
- home dashboard and subject catalogue
- searchable/filterable question bank
- timed and untimed exams, configurable exam modes
- answer explanations and references
- bookmarks, incorrect-answer review and spaced revision
- attempt history, analytics and progress goals
- subscription plans, entitlements and notifications
- offline cache with explicit sync state

### Admin
- secure sign-in and role verification
- operational dashboard and audit log
- subjects, categories, tags and question CRUD
- rich question editor: text, images, tables, options, correct answer, explanation, references and difficulty
- bulk import/export with validation and duplicate detection
- draft/review/published/archived content lifecycle
- user and role management
- subscription/entitlement oversight
- exam templates, analytics, announcements and notification campaigns
- moderation, content versioning and restore

## Question security
Question prompts and options may be readable by entitled authenticated learners. Correct answers and grading rules must not be exposed through a public table or client-readable API. Store answer keys separately and grade attempts in a trusted Edge Function or RPC with server-side authorization. Do not ship service-role credentials in either APK.

## Delivery phases
- Phase 1 — Foundation: separate Android modules, CI artifacts, architecture contract, secure database migration.
- Phase 2 — Identity: Supabase Auth, profiles, role provisioning, session lifecycle, RLS tests.
- Phase 3 — Content operations: admin question editor, taxonomy, media storage, review/publish workflow, import/export.
- Phase 4 — Learner product: full catalogue, exam engine, bookmarks, revision, analytics and offline sync.
- Phase 5 — Commercial: subscription catalogue, payment integration/webhooks, entitlements, invoices/receipts and notification service.
- Phase 6 — Production: automated tests, security review, observability, privacy policy, backups, signed release builds and Play Store preparation.

## Current implementation status
The admin Android module and its initial navigation/dashboard shell are established in Phase 1. Its metric values are intentionally placeholders until authenticated backend integration exists. The existing learner app is retained to avoid breaking its package identity. The SQL migration is a foundation and must be applied and tested in a Supabase project before production use.
