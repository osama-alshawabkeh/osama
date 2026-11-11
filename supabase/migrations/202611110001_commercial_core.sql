-- MultiChoice Medical commercial foundation.
-- Review against an existing database before applying. Intended for Supabase.
begin;

create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    avatar_path text,
    role text not null default 'student'
        check (role in ('student', 'content_editor', 'admin', 'owner')),
    account_status text not null default 'active'
        check (account_status in ('active', 'suspended', 'deleted')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create or replace function public.current_user_is_admin()
returns boolean language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
        where p.id = auth.uid()
          and p.role in ('admin', 'owner')
          and p.account_status = 'active'
    );
$$;

create or replace function public.current_user_can_edit_content()
returns boolean language sql stable security definer set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
        where p.id = auth.uid()
          and p.role in ('content_editor', 'admin', 'owner')
          and p.account_status = 'active'
    );
$$;

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
    insert into public.profiles (id, display_name)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'name', ''))
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created_multichoice on auth.users;
create trigger on_auth_user_created_multichoice
after insert on auth.users
for each row execute procedure public.handle_new_user();

create or replace function public.guard_profile_role_change()
returns trigger language plpgsql security definer set search_path = public
as $$
begin
    if new.role is distinct from old.role and not public.current_user_is_admin() then
        raise exception 'Only an administrator may change account roles';
    end if;
    if new.account_status is distinct from old.account_status and not public.current_user_is_admin() then
        raise exception 'Only an administrator may change account status';
    end if;
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists guard_profile_role_change on public.profiles;
create trigger guard_profile_role_change
before update on public.profiles
for each row execute procedure public.guard_profile_role_change();

create table if not exists public.subjects (
    id uuid primary key default gen_random_uuid(),
    parent_id uuid references public.subjects(id) on delete set null,
    slug text not null unique,
    title_ar text not null,
    title_en text,
    description text,
    icon_path text,
    sort_order integer not null default 0,
    is_published boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.questions_v2 (
    id uuid primary key default gen_random_uuid(),
    subject_id uuid not null references public.subjects(id) on delete restrict,
    stem text not null,
    explanation text not null default '',
    reference_text text,
    difficulty smallint not null default 2 check (difficulty between 1 and 5),
    status text not null default 'draft' check (status in ('draft', 'in_review', 'published', 'archived')),
    created_by uuid references public.profiles(id) on delete set null,
    updated_by uuid references public.profiles(id) on delete set null,
    version integer not null default 1 check (version > 0),
    published_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.question_options (
    id uuid primary key default gen_random_uuid(),
    question_id uuid not null references public.questions_v2(id) on delete cascade,
    option_key text not null check (option_key in ('A', 'B', 'C', 'D', 'E')),
    body text not null,
    sort_order smallint not null default 0,
    unique (question_id, option_key)
);

-- Correct answers are isolated from learner-readable question and option tables.
create table if not exists public.question_answer_keys (
    question_id uuid primary key references public.questions_v2(id) on delete cascade,
    correct_option_key text not null check (correct_option_key in ('A', 'B', 'C', 'D', 'E')),
    grading_note text,
    updated_at timestamptz not null default now()
);

create table if not exists public.exams (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text,
    subject_id uuid references public.subjects(id) on delete set null,
    duration_seconds integer check (duration_seconds is null or duration_seconds > 0),
    question_count integer not null default 0 check (question_count >= 0),
    access_level text not null default 'free' check (access_level in ('free', 'premium')),
    status text not null default 'draft' check (status in ('draft', 'published', 'archived')),
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.exam_questions (
    exam_id uuid not null references public.exams(id) on delete cascade,
    question_id uuid not null references public.questions_v2(id) on delete restrict,
    position integer not null check (position > 0),
    primary key (exam_id, question_id),
    unique (exam_id, position)
);

create table if not exists public.subscriptions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    provider text not null,
    provider_reference text unique,
    plan_code text not null,
    status text not null check (status in ('pending', 'active', 'past_due', 'cancelled', 'expired', 'refunded')),
    starts_at timestamptz,
    ends_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.exam_attempts (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    exam_id uuid references public.exams(id) on delete set null,
    status text not null default 'in_progress' check (status in ('in_progress', 'submitted', 'graded', 'abandoned')),
    total_questions integer not null default 0,
    correct_count integer,
    score_percent numeric(5,2),
    started_at timestamptz not null default now(),
    submitted_at timestamptz,
    graded_at timestamptz
);

create table if not exists public.exam_attempt_answers (
    attempt_id uuid not null references public.exam_attempts(id) on delete cascade,
    question_id uuid not null references public.questions_v2(id) on delete restrict,
    selected_option_key text check (selected_option_key in ('A', 'B', 'C', 'D', 'E')),
    is_correct boolean,
    answered_at timestamptz,
    primary key (attempt_id, question_id)
);

create table if not exists public.audit_logs (
    id bigint generated always as identity primary key,
    actor_id uuid references public.profiles(id) on delete set null,
    action text not null,
    entity_type text not null,
    entity_id text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists idx_subjects_published_sort on public.subjects(is_published, sort_order);
create index if not exists idx_questions_subject_status on public.questions_v2(subject_id, status);
create index if not exists idx_exams_status_access on public.exams(status, access_level);
create index if not exists idx_subscriptions_user_status on public.subscriptions(user_id, status, ends_at);
create index if not exists idx_attempts_user_started on public.exam_attempts(user_id, started_at desc);
create index if not exists idx_audit_logs_created on public.audit_logs(created_at desc);

alter table public.profiles enable row level security;
alter table public.subjects enable row level security;
alter table public.questions_v2 enable row level security;
alter table public.question_options enable row level security;
alter table public.question_answer_keys enable row level security;
alter table public.exams enable row level security;
alter table public.exam_questions enable row level security;
alter table public.subscriptions enable row level security;
alter table public.exam_attempts enable row level security;
alter table public.exam_attempt_answers enable row level security;
alter table public.audit_logs enable row level security;

drop policy if exists profiles_read_own_or_admin on public.profiles;
create policy profiles_read_own_or_admin on public.profiles
for select to authenticated using (id = auth.uid() or public.current_user_is_admin());

drop policy if exists profiles_update_own_or_admin on public.profiles;
create policy profiles_update_own_or_admin on public.profiles
for update to authenticated using (id = auth.uid() or public.current_user_is_admin())
with check (id = auth.uid() or public.current_user_is_admin());

drop policy if exists subjects_read_published_or_editor on public.subjects;
create policy subjects_read_published_or_editor on public.subjects
for select to authenticated using (is_published or public.current_user_can_edit_content());
drop policy if exists subjects_manage_editor on public.subjects;
create policy subjects_manage_editor on public.subjects
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists questions_read_published_or_editor on public.questions_v2;
create policy questions_read_published_or_editor on public.questions_v2
for select to authenticated using (status = 'published' or public.current_user_can_edit_content());
drop policy if exists questions_manage_editor on public.questions_v2;
create policy questions_manage_editor on public.questions_v2
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists options_read_published_or_editor on public.question_options;
create policy options_read_published_or_editor on public.question_options
for select to authenticated using (
    exists (select 1 from public.questions_v2 q where q.id = question_id and q.status = 'published')
    or public.current_user_can_edit_content()
);
drop policy if exists options_manage_editor on public.question_options;
create policy options_manage_editor on public.question_options
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists answer_keys_editor_only on public.question_answer_keys;
create policy answer_keys_editor_only on public.question_answer_keys
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists exams_read_published_or_editor on public.exams;
create policy exams_read_published_or_editor on public.exams
for select to authenticated using (status = 'published' or public.current_user_can_edit_content());
drop policy if exists exams_manage_editor on public.exams;
create policy exams_manage_editor on public.exams
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists exam_questions_read_published_or_editor on public.exam_questions;
create policy exam_questions_read_published_or_editor on public.exam_questions
for select to authenticated using (
    exists (select 1 from public.exams e where e.id = exam_id and e.status = 'published')
    or public.current_user_can_edit_content()
);
drop policy if exists exam_questions_manage_editor on public.exam_questions;
create policy exam_questions_manage_editor on public.exam_questions
for all to authenticated using (public.current_user_can_edit_content())
with check (public.current_user_can_edit_content());

drop policy if exists subscriptions_read_own_or_admin on public.subscriptions;
create policy subscriptions_read_own_or_admin on public.subscriptions
for select to authenticated using (user_id = auth.uid() or public.current_user_is_admin());

drop policy if exists attempts_read_own_or_admin on public.exam_attempts;
create policy attempts_read_own_or_admin on public.exam_attempts
for select to authenticated using (user_id = auth.uid() or public.current_user_is_admin());

drop policy if exists attempt_answers_read_own_or_admin on public.exam_attempt_answers;
create policy attempt_answers_read_own_or_admin on public.exam_attempt_answers
for select to authenticated using (
    exists (select 1 from public.exam_attempts a where a.id = attempt_id and (a.user_id = auth.uid() or public.current_user_is_admin()))
);

drop policy if exists audit_logs_admin_only on public.audit_logs;
create policy audit_logs_admin_only on public.audit_logs
for select to authenticated using (public.current_user_is_admin());

-- Grading, answer-key writes, subscriptions and audit writes belong to trusted server functions.
revoke all on public.question_answer_keys from anon, authenticated;
revoke insert, update, delete on public.subscriptions from anon, authenticated;
revoke insert, update, delete on public.exam_attempts from anon, authenticated;
revoke insert, update, delete on public.exam_attempt_answers from anon, authenticated;
revoke insert, update, delete on public.audit_logs from anon, authenticated;

commit;
