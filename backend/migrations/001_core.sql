-- Local PostgreSQL schema for the phone-hosted MultiChoice backend.
-- This schema is independent of Supabase auth and can run on plain PostgreSQL.

begin;

create table if not exists users (
    id uuid primary key default gen_random_uuid(),
    email text not null unique,
    password_hash text not null,
    display_name text not null default '',
    role text not null default 'student'
        check (role in ('student', 'content_editor', 'admin', 'owner')),
    status text not null default 'active'
        check (status in ('active', 'suspended')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists subjects (
    id uuid primary key default gen_random_uuid(),
    parent_id uuid references subjects(id) on delete set null,
    slug text not null unique,
    title_ar text not null,
    title_en text,
    description text,
    sort_order integer not null default 0,
    is_published boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists questions (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references subjects(id) on delete restrict,
    stem text not null,
    explanation text not null default '',
    reference_text text,
    difficulty smallint not null default 2 check (difficulty between 1 and 5),
    status text not null default 'draft'
        check (status in ('draft', 'in_review', 'published', 'archived')),
    version integer not null default 1 check (version > 0),
    created_by uuid references users(id) on delete set null,
    updated_by uuid references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists question_options (
    id uuid primary key default gen_random_uuid(),
    question_id uuid not null references questions(id) on delete cascade,
    option_key text not null check (option_key in ('A', 'B', 'C', 'D', 'E')),
    body text not null,
    sort_order smallint not null default 0,
    unique (question_id, option_key)
);

-- Never return this table from learner-facing question endpoints.
create table if not exists question_answer_keys (
    question_id uuid primary key references questions(id) on delete cascade,
    correct_option_key text not null check (correct_option_key in ('A', 'B', 'C', 'D', 'E')),
    updated_at timestamptz not null default now()
);

create table if not exists exams (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text,
    subject_id uuid references subjects(id) on delete set null,
    duration_seconds integer check (duration_seconds is null or duration_seconds > 0),
    access_level text not null default 'free' check (access_level in ('free', 'premium')),
    status text not null default 'draft' check (status in ('draft', 'published', 'archived')),
    created_by uuid references users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists exam_questions (
    exam_id uuid not null references exams(id) on delete cascade,
    question_id uuid not null references questions(id) on delete restrict,
    position integer not null check (position > 0),
    primary key (exam_id, question_id),
    unique (exam_id, position)
);

create table if not exists subscriptions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    provider text not null,
    provider_reference text unique,
    plan_code text not null,
    status text not null check (status in ('pending', 'active', 'past_due', 'cancelled', 'expired', 'refunded')),
    starts_at timestamptz,
    ends_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists exam_attempts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id) on delete cascade,
    exam_id uuid references exams(id) on delete set null,
    status text not null default 'in_progress'
        check (status in ('in_progress', 'submitted', 'graded', 'abandoned')),
    total_questions integer not null default 0,
    correct_count integer,
    score_percent numeric(5,2),
    started_at timestamptz not null default now(),
    submitted_at timestamptz,
    graded_at timestamptz
);

create table if not exists exam_attempt_answers (
    attempt_id uuid not null references exam_attempts(id) on delete cascade,
    question_id uuid not null references questions(id) on delete restrict,
    selected_option_key text check (selected_option_key in ('A', 'B', 'C', 'D', 'E')),
    is_correct boolean,
    answered_at timestamptz,
    primary key (attempt_id, question_id)
);

create table if not exists audit_logs (
    id bigint generated always as identity primary key,
    actor_id uuid references users(id) on delete set null,
    action text not null,
    entity_type text not null,
    entity_id text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_questions_subject_status on questions(subject_id, status);
create index if not exists idx_subscriptions_user_status on subscriptions(user_id, status, ends_at);
create index if not exists idx_attempts_user_started on exam_attempts(user_id, started_at desc);
create index if not exists idx_audit_logs_created on audit_logs(created_at desc);

commit;
