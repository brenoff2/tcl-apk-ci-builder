-- AnyTV user library for an exclusive Supabase Auth project.
-- Prepared only: not applied to unrelated projects.
create table if not exists public.anytv_library_items (
  user_id uuid not null references auth.users(id) on delete cascade,
  kind text not null check (kind in (
    'favorite','watched_movie','tracked_series','watched_episode',
    'preferred_genre','preferred_provider'
  )),
  item_key text not null check (length(item_key) between 1 and 160),
  payload jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false,
  primary key (user_id, kind, item_key)
);
create index if not exists anytv_library_user_changed
  on public.anytv_library_items(user_id, updated_at);
alter table public.anytv_library_items enable row level security;
revoke all on public.anytv_library_items from anon, authenticated;
grant select, insert, update on public.anytv_library_items to authenticated;
create policy "anytv_user_read" on public.anytv_library_items
 for select to authenticated using ((select auth.uid()) = user_id);
create policy "anytv_user_insert" on public.anytv_library_items
 for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "anytv_user_update" on public.anytv_library_items
 for update to authenticated using ((select auth.uid()) = user_id)
 with check ((select auth.uid()) = user_id);
-- Deletions use tombstones to support offline reconciliation.
-- No delete grant or delete policy: authenticated users set deleted=true instead.
