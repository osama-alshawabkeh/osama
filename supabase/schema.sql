create table if not exists public.questions (
  id bigint primary key,
  category text not null default 'عام',
  question text not null,
  option_a text not null,
  option_b text not null,
  option_c text not null,
  option_d text not null,
  correct_index smallint not null check (correct_index between 0 and 3),
  explanation text not null default ''
);

alter table public.questions enable row level security;

-- Public read access is suitable only for a non-sensitive question bank.
-- Do not store private exam material or answer keys that must remain secret.
create policy "Questions are readable by app users"
on public.questions for select
to anon, authenticated
using (true);
