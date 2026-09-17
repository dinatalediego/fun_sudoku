-- Fun Sudoku cloud persistence schema.
-- Apply to the dedicated Supabase project once it is created.

create table if not exists public.game_sessions (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    difficulty text not null check (difficulty in ('EASY', 'MEDIUM', 'HARD', 'EXPERT')),
    elapsed_seconds bigint not null check (elapsed_seconds >= 0),
    mistakes integer not null default 0 check (mistakes >= 0),
    hints_used integer not null default 0 check (hints_used >= 0),
    completed_at_ms bigint not null check (completed_at_ms >= 0),
    created_at timestamptz not null default now()
);

alter table public.game_sessions enable row level security;

grant select, insert, update, delete on public.game_sessions to authenticated;

create policy "game_sessions_select_own"
on public.game_sessions
for select
to authenticated
using ((select auth.uid()) = user_id);

create policy "game_sessions_insert_own"
on public.game_sessions
for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy "game_sessions_update_own"
on public.game_sessions
for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "game_sessions_delete_own"
on public.game_sessions
for delete
to authenticated
using ((select auth.uid()) = user_id);

create index if not exists game_sessions_user_completed_idx
    on public.game_sessions (user_id, completed_at_ms desc);
